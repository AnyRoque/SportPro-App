# SportPro ⚽

Aplicación Android para la gestión integral de equipos y academias de fútbol.
Curso **Desarrollo de Aplicaciones Móviles – Universidad ESAN (S-002)** · Prof. Luis Alberto Chang Uribe.

| Integrante | Módulo |
|---|---|
| Jelinek Albornoz Rosario | Acceso, equipos y jugadores (US-01 a US-05) |
| Juan Arturo Alvarado Ojeda | Administración y configuración (US-06 a US-10) |
| Jesús Daniel León Condori | Entrenamientos y estadísticas (US-11 a US-15) |
| Christ Andy Salva Grijalba | Partido: convocatoria, alineación y registro en vivo (US-16 a US-21) |
| Any Zobeidi Pariacuri Roque | Comunicación, comunidad e IA (US-22 a US-27) |

## Tecnologías
Kotlin · Jetpack Compose · MVVM (ViewModel + StateFlow) · Navigation Compose · Firebase Authentication · Cloud Firestore · (próximo) Cloud Storage, Cloud Messaging y Cloud Functions.

## Estructura (misma convención del proyecto del curso)
```
app/src/main/java/pe/edu/esan/sportpro/
├── MainActivity.kt
├── data/
│   ├── model/        UserModel, TeamModel, PlayerModel, Role, Catalogs
│   └── remote/       FirebaseAuthManager, TeamRepository, PlayerRepository
├── presentation/
│   ├── auth/         Login, Registro, Recuperar contraseña, AuthViewModel   (US-01)
│   ├── children/     Mis hijos, Registro de hijos                           (US-02)
│   ├── teams/        Mis equipos, Nuevo equipo (DT) · Validar equipos (ADM) (US-03, US-07)
│   ├── players/      Plantel, Perfil del jugador                            (US-04)
│   ├── home/ profile/ components/
│   └── navigation/   Routes, AppNavGraph, DrawerScaffold, SecuredScreen
├── ui/theme/
└── util/             Validators, FormValidators (con pruebas unitarias)
```
Cada módulo registra sus rutas en su propio archivo `*Nav.kt`; así cada integrante trabaja en su rama sin conflictos.

## Avance – Semana 7 (40 %)
- [x] Registro e inicio de sesión por rol (DT, JUG, PAD; el ADM no se autorregistra) + recuperar contraseña
- [x] Navegación con menú lateral según rol y bloqueo de pantallas de otro rol
- [x] DT: registro de equipos (quedan **Pendientes**), corregir y reenviar si son rechazados
- [x] ADM: aprobar/rechazar equipos con motivo
- [x] DT: plantel en tiempo real, búsqueda, filtro por posición, crear/editar/dar de baja jugadores
- [x] PAD: registro de hijos y solicitud de vínculo; el DT acepta o rechaza
- [x] Reglas de seguridad de Firestore (`firestore.rules`) según la matriz de privacidad
- [x] Diseño técnico: [tiempo real](docs/diseno-tiempo-real.md) · [integración segura con IA](docs/integracion-ia.md)

## Configuración de Firebase (una sola vez)
1. En [Firebase Console](https://console.firebase.google.com) crear el proyecto **SportPro**.
2. Agregar una app Android con el paquete **`pe.edu.esan.sportpro`** y descargar `google-services.json` en la carpeta `app/`.
3. **Authentication** → Método de acceso → habilitar **Correo electrónico/contraseña**.
4. **Firestore Database** → Crear base de datos → luego en **Reglas** pegar el contenido de `firestore.rules` y publicar.
5. **Crear el primer administrador** (no existe registro público de ADM):
   - Authentication → Usuarios → *Agregar usuario* (correo y contraseña). Copiar el **UID**.
   - Firestore → colección `users` → documento con ID = UID y campos:
     `name` (string), `lastName` (string), `email` (string), `phone` (string), `role` = `"ADM"`, `active` = `true`.
6. Abrir el proyecto en Android Studio → *Sync Project with Gradle Files* → ejecutar.

## Prueba rápida del flujo
1. Registrarse como **DT** → *Mis equipos* → *Nuevo equipo* (queda Pendiente).
2. Iniciar sesión como **ADM** → *Validar equipos* → Aprobar.
3. Como **DT** → abrir el equipo → agregar jugadores.
4. Registrarse como **PAD** → registrar un hijo en ese equipo → como **DT**, pestaña *Solicitudes* → Aceptar.

## Trabajo colaborativo
Ver [docs/flujo-git.md](docs/flujo-git.md) (ramas, commits y Pull Requests individuales).
