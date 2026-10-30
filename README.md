# ToDo · Lab 7 — Seguridad con Spring Security y JWT

**Escuela Colombiana de Ingeniería Julio Garavito** · Desarrollo y Operaciones de Software (DOSW)
**Caso de estudio:** App ToDo · Gestión de tareas
**Base:** [Laboratorio 6 · ToDo Full Stack](docs/lab6/LAB6.md)

| Repositorio | Contenido |
|---|---|
| [back-end-lab06](https://github.com/Miguelim101/back-end-lab06) | API REST Spring Boot, scripts SQL, diagramas, evidencias y este README general |
| [front-end-lab06](https://github.com/Miguelim101/front-end-lab06) | SPA React + Vite |

Enunciado y reglas del laboratorio: [requirements_part2.md](docs/lab7/requirements_part2.md) · [LAB7.md](docs/lab7/LAB7.md)

---

## Índice

1. [Alcance](#1-alcance)
2. [Estado inicial](#2-estado-inicial)
3. [Planeación (checklist)](#3-planeación-checklist)
4. [Requerimientos e historias de usuario](#4-requerimientos-e-historias-de-usuario)
5. [Diseño](#5-diseño)
6. [Flujo de autenticación JWT](#6-flujo-de-autenticación-jwt)
7. [Contrato de endpoints](#7-contrato-de-endpoints)
8. [Decisiones de seguridad](#8-decisiones-de-seguridad)
9. [Ejecución local](#9-ejecución-local)
10. [Desarrollo paso a paso y evidencias](#10-desarrollo-paso-a-paso-y-evidencias)
11. [Pruebas y cobertura](#11-pruebas-y-cobertura)
12. [Definition of Done](#12-definition-of-done)
13. [Informe y video](#13-informe-y-video)

---

## 1. Alcance

La aplicación ToDo del Lab 6 (React + Spring Boot + PostgreSQL en Docker) se extiende con **cuentas de usuario y tareas privadas**:

- Registro de usuarios e inicio de sesión con contraseñas almacenadas mediante **BCrypt**.
- Emisión de un **JWT firmado (HS256)** con duración de **30 minutos** y validación en cada petición protegida.
- Aislamiento por identidad: **cada usuario solo puede ver, crear, editar y eliminar sus propias tareas** (incluido el calendario semanal).
- Recurso administrativo `GET /api/v1/admin/summary` exclusivo para el rol `ADMIN`.
- Frontend con vistas de registro, inicio de sesión, tareas y cierre de sesión; el token se conserva **en memoria**.
- Pruebas unitarias y de integración para los flujos correctos y los intentos de acceso indebido.

**Fuera de alcance** (según el enunciado): despliegue en la nube, CI/CD, OAuth con Google, refresh tokens, MFA y recuperación de contraseña.

---

## 2. Estado inicial

Al iniciar el Lab 7 la aplicación cuenta con:

- **Back-end:** API REST `/api/v1/tasks` (CRUD completo + filtro `?from&to` para el calendario), capas Controller → Service → Repository → Entity, DTOs, manejo de errores `{status, message}` (400/404), CORS para `http://localhost:5173` y pruebas unitarias (Service y Controller) con JaCoCo ≥ 70 %.
- **Base de datos:** PostgreSQL 17 (`postgres:17-alpine`) en el contenedor `todo-postgres` con volumen persistente; esquema creado por `database/001_create_schema.sql` y `ddl-auto: validate`.
- **Front-end:** React 19 + Vite con `TaskForm`, `TaskList`, `TaskItem`, `TaskCalendar`, el hook `useTasks` y `taskApi.js`; pruebas con Vitest + React Testing Library.
- **Sin seguridad:** cualquier cliente puede consultar y modificar todas las tareas; no existe el concepto de usuario.

**Cambios previstos:** tabla `users` y propietario en `tasks`; Spring Security + OAuth2 Resource Server (JWT); controladores de autenticación, perfil y administración; filtrado por propietario en Service/Repository; vistas de login/registro y sesión en React.

Rama de trabajo: `feature/jwt-security` (en ambos repositorios).

| Evidencia | |
|---|---|
| PostgreSQL en Docker | ![docker ps](docs/evidence/p0_docker_ps.png) |
| CRUD inicial en React | ![CRUD inicial](docs/evidence/p0_crud_inicial.png) |
| Pruebas back-end iniciales | ![mvn verify](docs/evidence/p0_backend_tests_inicial.png) |
| Pruebas front-end iniciales | ![npm test](docs/evidence/p0_frontend_tests_inicial.png) |

---

## 3. Planeación (checklist)

> **Nota:** No implementé Jira dado que la historia de usuario ya estaba creada en el documento y la verdad vi más rápido hacerlo por checklist que crear el Jira (tengo que hacer proyecto, entiéndanme, además que son funcionalidades cortas).

La planeación conserva la misma organización que se habría llevado a Jira (épica → features → historias → tareas). Cada tarea se marca al completarse.

**Épica SEG · Seguridad con Spring Security y JWT**

#### Feature F1 · Gestión de cuentas

**HU-SEG-01 · Registro**
- [ ] Script `002_create_users.sql`
- [ ] Entidad y repositorio de usuarios
- [ ] DTOs `RegisterRequest` / `UserResponse`
- [ ] Registro con BCrypt y rol USER
- [ ] Manejo de correo duplicado (409)
- [ ] `RegisterForm` en React
- [ ] Pruebas PT-01, PT-02, PT-03

**HU-SEG-02 · Inicio de sesión**
- [ ] Configuración JWT (encoder/decoder)
- [ ] `TokenService`
- [ ] Login con 401 genérico
- [ ] `GET /users/me`
- [ ] `AuthContext` + `LoginForm`
- [ ] Header `Authorization` en `taskApi.js`
- [ ] Pruebas PT-04, PT-05

#### Feature F2 · Protección y aislamiento de datos

**HU-SEG-04 · Protección de recursos**
- [ ] Exigir JWT en `/tasks/**`
- [ ] Respuesta 401 en JSON
- [ ] Adaptar `TaskControllerTest`
- [ ] Pruebas PT-06, PT-08, PT-09

**HU-SEG-03 · Acceso a las tareas propias**
- [ ] Script `003_add_task_owner.sql` (migración de tareas previas)
- [ ] Relación `TaskEntity` → `UserEntity`
- [ ] Consultas por propietario en Repository/Service
- [ ] Usuario tomado del JWT en el Controller
- [ ] Calendario filtrado por propietario
- [ ] Pruebas de repositorio e integración PT-07, PT-10..PT-13, PT-18

#### Feature F3 · Autorización por roles

**HU-SEG-05 · Recurso administrativo**
- [ ] `GET /admin/summary`
- [ ] Regla `hasRole('ADMIN')`
- [ ] Respuesta 403 en JSON
- [ ] Procedimiento de ADMIN local
- [ ] Pruebas PT-14, PT-15

#### Feature F4 · Sesión en el frontend

**HU-SEG-06 · Sesión en React**
- [ ] Barra con usuario y "Cerrar sesión"
- [ ] Limpieza de estado al cerrar sesión
- [ ] Redirección al login ante 401
- [ ] Pruebas PT-16, PT-17

#### Feature F5 · Diseño y documentación
- [ ] Diagramas C4 (contexto, contenedores, componentes)
- [ ] Diagrama de clases
- [ ] Modelo ER
- [ ] README
- [ ] Informe `docs/informe-seguridad-jwt.md`

---

## 4. Requerimientos e historias de usuario

### 4.1 Requerimientos funcionales

| Código | Requerimiento | Resultado esperado | Estado |
|---|---|---|---|
| RF-SEG-01 | Registrar usuario | Crear cuenta con nombre, correo y contraseña. | ⬜ |
| RF-SEG-02 | Autenticar usuario | Validar credenciales y emitir JWT firmado. | ⬜ |
| RF-SEG-03 | Mantener sesión en React | Utilizar el token en las peticiones a los recursos protegidos. | ⬜ |
| RF-SEG-04 | Consultar perfil | Obtener los datos no sensibles del usuario autenticado. | ⬜ |
| RF-SEG-05 | Proteger el CRUD | Requerir token válido en los recursos de tareas. | ⬜ |
| RF-SEG-06 | Aislar tareas por usuario | Listar, consultar, crear, editar y eliminar solo tareas propias. | ⬜ |
| RF-SEG-07 | Gestionar autorización por roles | Reservar un endpoint administrativo exclusivamente al rol `ADMIN`. | ⬜ |
| RF-SEG-08 | Cerrar sesión en frontend | Limpiar el estado de autenticación y volver al inicio de sesión. | ⬜ |
| RF-SEG-09 | Manejar errores de seguridad | Diferenciar errores de validación, autenticación, autorización y recursos inexistentes. | ⬜ |
| RF-SEG-10 | Validar seguridad mediante pruebas | Cubrir rutas públicas, protegidas, token inválido y acceso cruzado. | ⬜ |

Las reglas funcionales y de seguridad (2.1) están en [requirements_part2.md](docs/lab7/requirements_part2.md#21-reglas-funcionales-y-de-seguridad).

### 4.2 Historias de usuario

| HU | Historia | Criterios de aceptación | Estado |
|---|---|---|---|
| HU-SEG-01 | **Como** visitante, **quiero** crear una cuenta con mis datos y una contraseña, **para** usar la aplicación de manera personal. | Registro exitoso con rol USER, contraseña con hash y respuesta sin contraseña ni hash. Correo duplicado: `409 Conflict` sin crear un segundo usuario. | ⬜ |
| HU-SEG-02 | **Como** usuario registrado, **quiero** iniciar sesión con correo y contraseña, **para** acceder a mis tareas. | Credenciales válidas: `200 OK` y JWT firmado con expiración. Contraseña incorrecta: `401 Unauthorized`, sin token. | ⬜ |
| HU-SEG-03 | **Como** usuario autenticado, **quiero** ver y modificar únicamente mis tareas, **para** mantener privada mi información. | `GET /tasks` devuelve solo las tareas propias. Operar sobre una tarea ajena: `404 Not Found` y la tarea queda sin cambios. | ⬜ |
| HU-SEG-04 | **Como** responsable del sistema, **quiero** que las APIs de tareas exijan un JWT válido, **para** impedir operaciones anónimas o falsificadas. | Sin `Authorization`: `401`. Token alterado o expirado: `401`. | ⬜ |
| HU-SEG-05 | **Como** administrador, **quiero** consultar un resumen general no sensible, **para** conocer la utilización del sistema. | Rol ADMIN: `200 OK` con métricas agregadas. Rol USER: `403 Forbidden`. | ⬜ |
| HU-SEG-06 | **Como** usuario autenticado, **quiero** acceder a mis tareas mientras dure la sesión y cerrarla al terminar, **para** controlar el uso de mi cuenta. | "Cerrar sesión" elimina el token del estado, oculta las tareas y muestra el inicio de sesión. | ⬜ |

Los escenarios Gherkin completos están en [requirements_part2.md](docs/lab7/requirements_part2.md#3-historias-de-usuario).

---

## 5. Diseño

Las fuentes de los diagramas están en [`docs/diagrams/`](docs/diagrams) (PlantUML; los C4 usan la librería estándar C4-PlantUML). En verde aparece lo **nuevo** del Lab 7 y en amarillo lo **modificado**.

Los `.svg` se regeneran con [PlantUML](https://plantuml.com/) (sin Graphviz, usando el motor Smetana): `java -jar plantuml.jar -tsvg -charset UTF-8 -Playout=smetana docs/diagrams/*.puml`

### 5.1 C4 · Nivel 1 · Contexto

![C4 contexto](docs/diagrams/c4_1_context.svg)

### 5.2 C4 · Nivel 2 · Contenedores

![C4 contenedores](docs/diagrams/c4_2_containers.svg)

### 5.3 C4 · Nivel 3 · Componentes

![C4 componentes](docs/diagrams/c4_3_components.svg)

### 5.4 Diagrama de clases

![Diagrama de clases](docs/diagrams/class_diagram.svg)

| Responsabilidad (LAB7 §5.2) | Dónde está |
|---|---|
| Usuario / entidad de persistencia | `entity/UserEntity`, `entity/Role` |
| Tarea / entidad de persistencia | `entity/TaskEntity` (relación `owner`) |
| Repositorios | `repository/UserRepository`, `repository/TaskRepository` |
| Registro y autenticación | `service/AuthService`, `service/AuthServiceImpl` |
| Emisión y validación de JWT | `service/TokenService` (emisión), `config/JwtConfig` (encoder, decoder, validadores, conversor de roles) |
| Configuración y reglas de Spring Security | `config/SecurityConfig` |
| Controladores REST | `AuthController`, `UserController`, `AdminController`, `TaskController` |
| DTOs | `RegisterRequest`, `LoginRequest`, `UserResponse`, `TokenResponse`, `AdminSummaryResponse`, `TaskCreateRequest`, `TaskUpdateRequest`, `TaskResponse` |
| Manejo centralizado de excepciones | `exception/GlobalExceptionHandler` + entry point / access denied handler de `SecurityConfig` |

### 5.5 Modelo entidad-relación

![Modelo ER](docs/diagrams/er_diagram.svg)

Migraciones en [`database/`](database):

| Script | Contenido |
|---|---|
| `001_create_schema.sql` | Tabla `tasks` (Lab 6) |
| `002_create_users.sql` | Tabla `users` (HU-SEG-01) |
| `003_add_task_owner.sql` | `tasks.user_id` + migración de tareas previas (HU-SEG-03) |

---

## 6. Flujo de autenticación JWT

```mermaid
sequenceDiagram
    actor U as Usuario
    participant R as React
    participant A as API /auth/login
    participant S as AuthService
    participant DB as PostgreSQL
    participant J as TokenService (JwtEncoder)

    U->>R: Ingresa correo y contraseña
    R->>A: POST /api/v1/auth/login
    A->>S: Validar credenciales
    S->>DB: Buscar usuario y hash
    DB-->>S: Usuario registrado
    S->>S: Verificar contraseña con PasswordEncoder (BCrypt)
    S->>J: Generar JWT firmado (HS256, 30 min)
    J-->>A: Token y expiración
    A-->>R: 200 OK + JWT
    R-->>U: Vista de tareas
```

---

## 7. Contrato de endpoints

_Se completa a medida que se implementa cada historia._

---

## 8. Decisiones de seguridad

_Se completa a medida que se implementa cada historia._

---

## 9. Ejecución local

### 9.1 Requisitos

- Docker (contenedor `todo-postgres` con la imagen `postgres:17-alpine` del Lab 6). También lo usan las pruebas de integración (Testcontainers).
- Java 21+ y Maven (o el wrapper `./mvnw`).
- Node.js + npm.

### 9.2 Variables de entorno

Copiar [`.env.example`](.env.example) como `.env` en la raíz de `back-end-lab06/` (está excluido de Git) y completar los valores. Spring Boot lo importa automáticamente.

| Variable | Descripción |
|---|---|
| `DB_URL` | URL JDBC de PostgreSQL (por defecto `jdbc:postgresql://localhost:5432/todo_db`) |
| `DB_USER` / `DB_PASSWORD` | Credenciales de la base de datos |
| `JWT_SECRET` | Llave de firma del JWT, en Base64 y de al menos 32 bytes. Generarla con `openssl rand -base64 32`. **Nunca se sube al repositorio.** |

### 9.3 Pasos

_Se completa a medida que se implementa cada historia (migraciones, back-end y front-end)._

---

## 10. Desarrollo paso a paso y evidencias

Todas las capturas están en [`docs/evidence/`](docs/evidence).

### Paso 0 · Punto de partida y planeación (Parte 1)

1. Se verificó que Docker + PostgreSQL + Spring Boot + React funcionaran antes de modificar código (ver [Estado inicial](#2-estado-inicial)).
2. Se creó la rama `feature/jwt-security` en ambos repositorios.
3. Se organizó la épica de seguridad con sus features, historias y tareas en un checklist (ver [Planeación](#3-planeación-checklist)).
4. Se documentaron el estado inicial y el alcance en este README.
5. Se elaboraron los diagramas previstos, marcando los componentes nuevos y modificados (ver [Diseño](#5-diseño)).
6. Ajustes de base: la configuración pasó de `application.properties` a `application.yaml`, se regeneró el Maven wrapper y se agregó `.env.example`.

---

## 11. Pruebas y cobertura

_Se completa a medida que se implementa cada historia (matriz PT-01..PT-18 con la prueba que cubre cada caso, comandos y cobertura JaCoCo)._

---

## 12. Definition of Done

- [ ] El CRUD original continúa funcionando para usuarios autenticados.
- [ ] Es posible registrarse e iniciar sesión.
- [ ] Las contraseñas se almacenan de forma segura.
- [ ] El JWT está firmado, vence y se valida en el servidor.
- [ ] Los endpoints de tareas rechazan peticiones sin autenticación.
- [ ] Ningún usuario puede ver, modificar o eliminar tareas ajenas.
- [ ] El rol `USER` no puede acceder al recurso exclusivo `ADMIN`.
- [ ] React consume recursos protegidos y permite cerrar sesión.
- [ ] Los tests nuevos y los del laboratorio anterior finalizan correctamente.
- [ ] Los diagramas C4, de clases y de datos se encuentran actualizados.
- [ ] Los secretos están fuera del repositorio.
- [ ] Se publicaron el informe y el video de funcionamiento local. _(El video no se realiza en esta entrega.)_

---

## 13. Informe y video

- Informe con las preguntas de comprensión: [`docs/informe-seguridad-jwt.md`](docs/informe-seguridad-jwt.md) _(se publica al cierre)_.