# Flujo de trabajo en GitHub

## Ramas
| Rama | Uso |
|---|---|
| `main` | Versión estable. **No se hace push directo**: todo entra por Pull Request. |
| `feature/<us>-<descripcion>` | Una rama por integrante y por historia o funcionalidad. |
| `docs/<tema>` | Documentación. |

Configurar en GitHub → Settings → Branches → *Add rule* para `main`: “Require a pull request before merging” (1 aprobación).

## Convención de commits
```
feat(US-03): formulario de registro de equipos
fix(US-04): validar número de camiseta repetido
docs: diseño técnico de eventos en tiempo real
```

## Pasos para cada integrante
```bash
git clone https://github.com/<usuario>/<repo>.git
cd <repo>
git checkout main && git pull
git checkout -b feature/us03-equipos          # tu rama
# ... copiar/editar tus archivos ...
git add .
git commit -m "feat(US-03): registro de equipos por DT con estado pendiente"
git push -u origin feature/us03-equipos
```
Luego en GitHub: **Compare & pull request** → completar la plantilla → pedir revisión a un compañero → *Merge*.
Antes de empezar otra tarea: `git checkout main && git pull`.

## Pull Requests de la Semana 7
Orden de merge: **PR 1 primero**; los PR 2 a 5 son independientes (cada uno reemplaza solo su archivo `*Nav.kt`); el PR 6 puede ir en cualquier momento.

| # | Integrante | Rama | Contenido |
|---|---|---|---|
| 1 | Any | `feature/base-proyecto` | Configuración Gradle/Firebase, tema, modelos, repositorios, validaciones + pruebas, componentes, navegación con menú por rol, `AuthViewModel`, inicio, perfil, `firestore.rules`, README. Los módulos aparecen como “En construcción”. |
| 2 | Jelinek | `feature/us01-us02-acceso-hijos` | Login, Registro, Recuperar contraseña (US-01) · Mis hijos y Registro de hijos (US-02) |
| 3 | Christ | `feature/us03-equipos` | Mis equipos y Nuevo equipo/Corregir (US-03) · `docs/diseno-tiempo-real.md` |
| 4 | Juan Arturo | `feature/us07-validacion-equipos` | Validar equipos por el ADM (US-07) |
| 5 | Jesús | `feature/us04-jugadores` | Plantel, formulario y perfil del jugador (US-04) |
| 6 | Any | `docs/integracion-ia` | `docs/integracion-ia.md` |

> Para esta entrega, Christ y Jesús apoyan el módulo 1 (equipos y jugadores), que es lo que pide el 40 %. Sus módulos (partido en vivo y entrenamientos) empiezan en la siguiente iteración; el diseño de tiempo real ya queda a cargo de Christ.
