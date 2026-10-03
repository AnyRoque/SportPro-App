# Diseño técnico: integración segura con el proveedor de IA

**Historias:** US-25 (generar resumen), US-26 (revisar y aprobar), US-27 (ver resumen publicado).
**Alcance (enunciado):** la IA solo genera un resumen narrativo del partido a partir de los eventos registrados. No hay sugerencias de alineación ni chatbot RAG.

---

## 1. Principios

1. **La clave de la API nunca está en la app.** Si estuviera en el APK, cualquiera podría extraerla. La llamada a la IA se hace desde el backend.
2. **Mínimo de datos.** A la IA solo se envían los hechos del partido. No se envían DNI, teléfonos, correos, datos físicos ni datos de padres.
3. **La IA no es la fuente de verdad.** Los eventos registrados lo son. El backend valida que el texto no contradiga esos datos.
4. **Un humano aprueba.** Todo resumen nace como `PENDIENTE_REVISION` y solo el DT puede publicarlo.

## 2. Arquitectura

```
 App Android (DT)                 Firebase                              Proveedor de IA
 ────────────────                 ────────                              ───────────────
 [Generar resumen] ──callable──► Cloud Function generateMatchSummary
   (Firebase Auth                1. Verifica auth.uid == match.dtUid
    + App Check)                 2. Verifica status == FINALIZADO
                                 3. Lee eventos ACTIVO + alineación
                                 4. Arma JSON mínimo (sin datos sensibles)
                                 5. Lee la API key de Secret Manager ──HTTPS──► Modelo LLM
                                 6. Valida la respuesta  ◄───────────────────── JSON
                                 7. Guarda summaries/{id} (PENDIENTE_REVISION)
 ◄──────── resultado ──────────
 Pantalla de revisión (US-26) ─► update: texto editado + APROBADO/PUBLICADO
```

- **Cloud Functions (2.ª gen), función *callable*:** Firebase entrega `context.auth` verificado, así que no hace falta manejar tokens a mano.
- **Secret Manager** (`defineSecret("AI_API_KEY")`): la clave no está en el código ni en el repositorio.
- **App Check (Play Integrity):** solo nuestra app firmada puede invocar la función.
- **Proveedor:** cualquier LLM con API HTTP (Gemini, Claude, OpenAI). El diseño no depende del proveedor; se cambia en un solo archivo (`aiClient.ts`).
- *Requisito:* Cloud Functions necesita el plan **Blaze** de Firebase, con capa gratuita. Alternativa sin servidor propio: **Firebase AI Logic** (Gemini) con App Check, que tampoco expone la clave en la app.

## 3. Datos que se envían a la IA

```json
{
  "partido": { "equipo": "Halcones FC", "rival": "Águilas", "fecha": "2026-11-07",
               "categoria": "Sub-15", "resultado": { "propio": 2, "rival": 1 } },
  "alineacion": { "titulares": ["J. Pérez", "L. Rojas", "..."], "suplentes": ["..."] },
  "eventos": [
    { "min": 12, "tipo": "Gol", "lado": "PROPIO", "jugador": "L. Rojas" },
    { "min": 30, "tipo": "Tarjeta amarilla", "lado": "RIVAL", "jugador": null },
    { "min": 55, "tipo": "Cambio", "lado": "PROPIO", "sale": "J. Pérez", "entra": "M. Díaz" }
  ]
}
```
Solo se envían los eventos con estado `ACTIVO`; los anulados quedan fuera. Los nombres se envían abreviados (inicial y apellido).

## 4. Prompt (versionado en el backend)

```
Eres un redactor deportivo. Escribe el resumen de un partido de fútbol formativo
usando EXCLUSIVAMENTE los datos del JSON.
Reglas:
- No inventes jugadores, jugadas, minutos ni resultados que no estén en el JSON.
- Si un evento no tiene jugador, no le asignes uno.
- Separa "hechos" (lo que dicen los datos) de "analisis" (interpretación breve y prudente).
- Tono positivo y respetuoso: son menores de edad. No critiques a jugadores individuales.
- Responde SOLO con JSON: {"hechos": string, "analisis": string, "jugadores_mencionados": [string]}
```

## 5. Validación de la respuesta (antes de guardar)

| Verificación | Si falla |
|---|---|
| La respuesta es JSON válido con `hechos` y `analisis` | Reintento automático (máx. 1) y luego error "Reintentar" |
| Cada nombre en `jugadores_mencionados` existe en la alineación o en los eventos | Se guarda con advertencia "Menciona jugadores no registrados" |
| El marcador que aparece en `hechos` coincide con `resultado` | Se guarda con advertencia |
| Hay al menos Inicio, Fin y un evento adicional | No se llama a la IA y se muestra "Eventos insuficientes" |
| Límite de 3 generaciones por partido | Se muestra "Alcanzaste el límite de regeneraciones" |

## 6. Documento `summaries/{summaryId}`

| Campo | Descripción |
|---|---|
| matchId, teamId | |
| status | `PENDIENTE_REVISION` · `PUBLICADO` · `RECHAZADO` · `DESACTUALIZADO` |
| aiFacts, aiAnalysis | texto original de la IA (no se modifica) |
| finalFacts, finalAnalysis | texto editado por el DT |
| warnings | lista de advertencias de la validación |
| model, promptVersion, generation | trazabilidad (modelo usado, versión del prompt, intento 1-3) |
| createdBy, createdAt, approvedBy, approvedAt | |

El original y la versión final se guardan juntos para la **evaluación documentada** de la entrega final (utilidad, omisiones, errores, información inventada y correcciones).
Si se edita un evento después de generar el resumen, una Cloud Function lo marca como `DESACTUALIZADO`.

## 7. Esqueleto de la Cloud Function (TypeScript)

```ts
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { getFirestore, FieldValue } from "firebase-admin/firestore";

const AI_API_KEY = defineSecret("AI_API_KEY");

export const generateMatchSummary = onCall(
  { secrets: [AI_API_KEY], enforceAppCheck: true, region: "us-central1" },
  async (req) => {
    const uid = req.auth?.uid;
    const { matchId } = req.data as { matchId: string };
    if (!uid) throw new HttpsError("unauthenticated", "Inicia sesión");

    const db = getFirestore();
    const match = (await db.doc(`matches/${matchId}`).get()).data();
    if (!match || match.dtUid !== uid) throw new HttpsError("permission-denied", "Solo el DT del partido");
    if (match.status !== "FINALIZADO") throw new HttpsError("failed-precondition", "El partido no ha finalizado");

    const events = (await db.collection(`matches/${matchId}/events`)
      .where("status", "==", "ACTIVO").orderBy("minute").get()).docs.map(d => d.data());
    if (events.length < 3) throw new HttpsError("failed-precondition", "Eventos insuficientes");

    const payload = buildMinimalPayload(match, events);          // sin datos sensibles
    const ai = await callModel(AI_API_KEY.value(), PROMPT_V1, payload); // aiClient.ts
    const warnings = validate(ai, payload);

    const ref = await db.collection("summaries").add({
      matchId, teamId: match.teamId, status: "PENDIENTE_REVISION",
      aiFacts: ai.hechos, aiAnalysis: ai.analisis, finalFacts: ai.hechos, finalAnalysis: ai.analisis,
      warnings, model: ai.model, promptVersion: "v1",
      createdBy: uid, createdAt: FieldValue.serverTimestamp(),
    });
    return { summaryId: ref.id, warnings };
  }
);
```

**En Android** (`SummaryRepository`):
```kotlin
val result = Firebase.functions.getHttpsCallable("generateMatchSummary")
    .call(mapOf("matchId" to matchId)).await()
```

## 8. Reglas de seguridad para `summaries`

```
match /summaries/{id} {
  allow read:   if esMiembroDelEquipo(resource.data.teamId)
                && (resource.data.status == "PUBLICADO" || esDT(resource.data.teamId));
  allow create: if false;                       // solo la Cloud Function (Admin SDK)
  allow update: if esDT(resource.data.teamId)
                && request.resource.data.aiFacts == resource.data.aiFacts;   // el original no se toca
}
```

## 9. Riesgos y mitigación

| Riesgo | Mitigación |
|---|---|
| Fuga de la clave | Secret Manager + llamada desde el backend + App Check |
| La IA inventa datos | Prompt restrictivo, validación de nombres y marcador, y aprobación humana obligatoria |
| Exposición de datos de menores | Payload mínimo, nombres abreviados, sin datos de contacto ni físicos |
| Costos o abuso | Límite de 3 generaciones por partido, App Check y solo el DT puede invocar |
| El proveedor falla o tarda | Timeout de 30 s, mensaje "Reintentar" y estado de carga en la app |
