# Diseño técnico: registro de eventos del partido en tiempo real

**Historias:** US-09 (catálogo de eventos), US-16 (programar partido), US-19 (registro en vivo), US-20 (edición/anulación con trazabilidad), US-21 (marcador en vivo sin recarga).
**Stack:** Android (Kotlin + Jetpack Compose + MVVM) · Cloud Firestore (listeners en tiempo real + caché offline) · Firebase Auth · Cloud Functions · FCM.

---

## 1. Objetivo y restricciones

| Requisito | Decisión de diseño |
|---|---|
| Un operador registra eventos desde el celular mientras ocurre el partido | Consola en vivo con botones rápidos; un evento se registra en un toque y se completa con un *bottom sheet* |
| Los demás dispositivos ven marcador y cronología **sin recargar** | `addSnapshotListener` de Firestore sobre el partido y sus eventos (objetivo < 2 s) |
| Catálogo de eventos configurable | Colección `eventTypes`; la consola lee el catálogo activo en tiempo real |
| Corregir/anular sin perder trazabilidad | Los eventos **nunca se borran**: cambian de estado y cada cambio se guarda en `audit` |
| Datos reales incompletos (jugador omitido, minuto tarde, sin conexión) | Campos opcionales con banderas (`needsCompletion`, `minuteCorrected`) + caché offline de Firestore |
| Privacidad de menores | El partido solo lo leen los usuarios del equipo (`memberUids`) |

## 2. Modelo de datos (Firestore)

```
eventTypes/{typeId}                      ← catálogo (US-09)
matches/{matchId}                        ← partido (US-16)
matches/{matchId}/events/{eventId}       ← línea de tiempo (US-19)
matches/{matchId}/events/{eventId}/audit/{auditId}   ← bitácora (US-20)
```

### `eventTypes/{typeId}`
| Campo | Tipo | Ejemplo |
|---|---|---|
| name | string | "Gol" |
| icon / color | string | "goal" / "#2E7D32" |
| requiresTeam | bool | true |
| requiresPlayer | bool | true |
| requiresSecondPlayer | bool | false (true en "Cambio": sale / entra) |
| affectsScore | bool | true |
| statKey | string? | "goals", "yellowCards", "redCards", "fouls" |
| active | bool | true |

### `matches/{matchId}`
| Campo | Tipo | Descripción |
|---|---|---|
| teamId, teamName, rival | string | |
| dtUid, operatorUid | string | quién puede registrar eventos |
| memberUids | array<string> | DT + jugadores + padres del equipo (lectura) |
| modality | int | 7, 9 u 11 |
| periodMinutes | int | duración de cada tiempo |
| status | string | `PROGRAMADO` · `EN_CURSO` · `ENTRETIEMPO` · `FINALIZADO` · `CANCELADO` |
| period | int | 1 o 2 |
| clock | map | `{ startedAt: Timestamp, accumulatedSec: int, running: bool }` |
| score | map | `{ own: int, rival: int }` (lo recalcula la Cloud Function) |
| lastEventAt | Timestamp | para ordenar y para notificaciones |

### `matches/{matchId}/events/{eventId}`
| Campo | Tipo | Descripción |
|---|---|---|
| eventId | string | **UUID generado en el celular** (idempotente ante reintentos) |
| typeId, typeName | string | copia del nombre para mostrar aunque el tipo cambie |
| minute | int | minuto del partido |
| minuteCorrected | bool | true si el operador corrigió el minuto (registro tardío) |
| period | int | 1 o 2 |
| side | string | `OWN` o `RIVAL` |
| playerId / playerName | string? | null si "Jugador no identificado" |
| playerInId / playerOutId | string? | solo para "Cambio" |
| needsCompletion | bool | true si falta un dato obligatorio (se completa luego) |
| note | string | observación opcional |
| status | string | `ACTIVO` · `ANULADO` |
| version | int | se incrementa en cada edición (control de concurrencia) |
| createdBy | string | uid del operador |
| createdAt | Timestamp | `serverTimestamp()` |
| clientCreatedAt | Timestamp | hora del celular (sirve si se registró sin conexión) |

### `.../events/{eventId}/audit/{auditId}`
`{ action: "CREAR"|"EDITAR"|"ANULAR", by, at, reason, before: {...}, after: {...} }`. Es de solo agregado (no se edita ni se borra).

## 3. Flujo en tiempo real

```
 Operador (DT)                         Firestore                          Espectadores (JUG/PAD/DT)
 ─────────────                         ─────────                          ─────────────────────────
 Toca "Gol" ─► ViewModel arma evento
               (UUID, minuto calculado)
               batch.set(event) ───────► events/{id}  ──snapshot──► Línea de tiempo + animación
               (hasPendingWrites=true                │
                si no hay red)                       ▼
                                          Cloud Function onEventWrite
                                          recalcula score y estadísticas
                                                     │
                                          matches/{id}.score ──snapshot──► Marcador actualizado
                                                     │
                                          FCM (gol / roja) ─────────────► Notificación push
```

1. **Registro:** el `LiveMatchViewModel` crea el evento con un `eventId` UUID y lo escribe con `set()` (no `add()`), así un reintento no duplica el evento.
2. **Escucha:** cada dispositivo abre dos listeners:
   - `matches/{id}` → marcador, estado y reloj.
   - `matches/{id}/events` ordenado por `period, minute, createdAt` → línea de tiempo.
   Los listeners se exponen como `Flow` con `callbackFlow` (mismo patrón que `TeamRepository.observeTeamsByDt`) y la UI los consume con `StateFlow`.
3. **Marcador consistente:** el marcador no se suma en el celular. Una Cloud Function (`onDocumentWritten` en `events`) recalcula `score` y las estadísticas leyendo **todos los eventos ACTIVO**. Al ser un recálculo completo, el resultado es el mismo aunque los eventos lleguen desordenados, se editen o se anulen.
4. **Reloj sin tráfico:** no se escribe cada segundo. Se guarda `startedAt` y `accumulatedSec`, y cada celular calcula `minuto = ⌊(ahora − startedAt + accumulatedSec) / 60⌋ + 1`. Pausar o iniciar el segundo tiempo es una sola escritura.

## 4. Datos sucios y casos límite

| Caso | Comportamiento |
|---|---|
| **Sin conexión** | Firestore guarda las escrituras en su caché local y las envía en orden al reconectar. La UI muestra "Pendiente de sincronizar" mientras `metadata.hasPendingWrites` sea `true`. |
| **Minuto registrado tarde** | El operador edita el minuto antes de confirmar → `minuteCorrected = true`. La línea de tiempo ordena por minuto, no por hora de llegada. |
| **Jugador omitido** | Se permite "Jugador no identificado" → `needsCompletion = true`. Aparece en una lista "Por completar" al finalizar. |
| **Doble toque / reintento** | El `eventId` UUID hace que la escritura sea idempotente. |
| **Dos operadores editan a la vez** | La edición usa una transacción que compara `version`. Si cambió, se muestra "El evento fue modificado, vuelve a intentarlo". |
| **Segunda amarilla** | El ViewModel genera automáticamente la "Tarjeta roja" asociada. Un jugador expulsado no aparece en los selectores. |
| **Cambio inválido** | Solo se puede elegir "sale" entre los que están en campo y "entra" entre los suplentes. |
| **Edición después del partido** | Se permite hasta 24 h después de `FINALIZADO`; luego, solo lectura. |

## 5. Edición y anulación con trazabilidad (US-20)

```kotlin
suspend fun annulEvent(matchId: String, eventId: String, reason: String, uid: String) =
    firestore.runTransaction { tx ->
        val ref = events(matchId).document(eventId)
        val before = tx.get(ref)
        tx.update(ref, mapOf("status" to "ANULADO", "version" to (before.getLong("version") ?: 0) + 1))
        tx.set(ref.collection("audit").document(), mapOf(
            "action" to "ANULAR", "by" to uid, "at" to FieldValue.serverTimestamp(),
            "reason" to reason, "before" to before.data, "after" to mapOf("status" to "ANULADO")
        ))
    }.await()
```
El evento anulado sigue en la línea de tiempo (tachado) y la Cloud Function recalcula el marcador.

## 6. Reglas de seguridad (resumen)

```
match /matches/{matchId} {
  allow read:   if request.auth.uid in resource.data.memberUids;
  allow update: if request.auth.uid in [resource.data.dtUid, resource.data.operatorUid];
  match /events/{eventId} {
    allow read:   if request.auth.uid in get(/databases/$(database)/documents/matches/$(matchId)).data.memberUids;
    allow create: if esOperador(matchId) && partidoEnCurso(matchId)
                  && request.resource.data.createdBy == request.auth.uid;
    allow update: if esOperador(matchId) && request.resource.data.version == resource.data.version + 1;
    allow delete: if false;   // nunca se borra: se anula
    match /audit/{auditId} { allow create: if esOperador(matchId); allow update, delete: if false; }
  }
}
```

## 7. Componentes Android

| Capa | Clase | Responsabilidad |
|---|---|---|
| data/model | `MatchModel`, `MatchEvent`, `EventType`, `AuditEntry` | Documentos de Firestore |
| data/remote | `MatchRepository` | `observeMatch()`, `observeEvents()` (callbackFlow), `registerEvent()`, `editEvent()`, `annulEvent()` |
| presentation/match | `LiveConsoleViewModel` | Reloj local, validaciones (segunda amarilla, expulsados, cambios), cola de pendientes |
| presentation/match | `LiveConsoleScreen` | Marcador, cronómetro, grilla de eventos, *bottom sheet* |
| presentation/match | `LiveMatchScreen` | Vista de espectador: marcador, línea de tiempo, banner sin conexión |
| functions | `onEventWrite` | Recalcula marcador/estadísticas y envía FCM en gol o roja |

## 8. Plan de pruebas (Semana 12)

1. Partido de práctica real con video: un operador registra y dos celulares observan.
2. Medir la latencia operador → espectador (meta < 2 s).
3. Activar modo avión en el operador, registrar 3 eventos y reconectar: deben llegar en orden y sin duplicados.
4. Registrar un evento con minuto tarde y otro sin jugador; completarlos al final.
5. Anular un gol: el marcador se recalcula en todos los dispositivos y la bitácora muestra quién, cuándo y por qué.
