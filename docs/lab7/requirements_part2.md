# Laboratorio de extensión · App ToDo: seguridad con Spring Security y JWT

**Escuela Colombiana de Ingeniería Julio Garavito**  
**Asignatura:** Desarrollo y Operaciones de Software (DOSW)  
**Caso de estudio:** App ToDo · Gestión de tareas  
**Base del laboratorio:** [Lab6-AppToDo](docs/lab6/LAB6.md)  
**Tema central:** autenticación, autorización, JSON Web Tokens (JWT) y protección de APIs REST.

---


# 2. Requerimientos funcionales

| Código | Requerimiento | Resultado esperado |
|---|---|---|
| RF-SEG-01 | Registrar usuario | Crear cuenta con nombre, correo y contraseña. |
| RF-SEG-02 | Autenticar usuario | Validar credenciales y emitir JWT firmado. |
| RF-SEG-03 | Mantener sesión en React | Utilizar el token en las peticiones a los recursos protegidos. |
| RF-SEG-04 | Consultar perfil | Obtener los datos no sensibles del usuario autenticado. |
| RF-SEG-05 | Proteger el CRUD | Requerir token válido en los recursos de tareas. |
| RF-SEG-06 | Aislar tareas por usuario | Listar, consultar, crear, editar y eliminar solo tareas propias. |
| RF-SEG-07 | Gestionar autorización por roles | Reservar un endpoint administrativo exclusivamente al rol `ADMIN`. |
| RF-SEG-08 | Cerrar sesión en frontend | Limpiar el estado de autenticación y volver al inicio de sesión. |
| RF-SEG-09 | Manejar errores de seguridad | Diferenciar errores de validación, autenticación, autorización y recursos inexistentes. |
| RF-SEG-10 | Validar seguridad mediante pruebas | Cubrir rutas públicas, protegidas, token inválido y acceso cruzado. |

## 2.1 Reglas funcionales y de seguridad

1. El correo identifica de manera única a cada usuario; se normaliza para evitar duplicados equivalentes.
2. No se permiten cuentas con correo inválido, nombre vacío ni contraseñas inferiores a **8 caracteres**.
3. La contraseña **nunca** debe almacenarse ni devolverse en texto plano. Se debe utilizar un `PasswordEncoder` seguro, por ejemplo BCrypt.
4. El registro público crea exclusivamente usuarios con rol `USER`. Ninguna petición pública podrá elegir `ADMIN`.
5. Las credenciales válidas producen un token firmado con identidad (`sub`), emisor (`iss`), fecha de emisión (`iat`) y expiración (`exp`). Si se incluyen roles, los claims deben verificarse y mapearse a autoridades de Spring Security.
6. Para este laboratorio el JWT tendrá una duración de **30 minutos**. Cuando expire, deberá ser rechazado y el usuario tendrá que iniciar sesión nuevamente.
7. El backend nunca debe confiar en un identificador de usuario recibido desde el formulario o la URL para decidir a quién pertenece una tarea. Debe obtener la identidad de la autenticación validada.
8. La respuesta de creación/consulta de usuario no debe incluir `password`, `passwordHash` ni secretos del servidor.
9. El token debe ser validado **en el backend**; ocultar botones en React no constituye autorización.
10. Las claves de firma JWT no se almacenarán en el repositorio. Se configurarán mediante variables de entorno o un archivo local excluido de Git.
11. No se deben registrar tokens, contraseñas ni cabeceras `Authorization` completas en los logs.
12. Se conservan las operaciones y reglas de negocio del CRUD anterior; la nueva protección se aplica a todas ellas.

---

# 3. Historias de usuario

## HU-SEG-01 · Registro

**Como** visitante de ToDo, **quiero** crear una cuenta con mis datos y una contraseña, **para** comenzar a utilizar la aplicación de manera personal.

**Criterios de aceptación (Gherkin)**

```gherkin
Escenario: Registro exitoso
  Dado que no existe una cuenta con el correo "ana@example.com"
  Cuando registro mi nombre, correo y una contraseña válida
  Entonces se crea una cuenta con rol USER
  Y la contraseña se almacena mediante un hash seguro
  Y la respuesta no contiene la contraseña ni su hash

Escenario: Correo duplicado
  Dado que ya existe una cuenta con el correo "ana@example.com"
  Cuando intento registrarme nuevamente con ese correo
  Entonces la API responde 409 Conflict
  Y no se crea un segundo usuario
```

## HU-SEG-02 · Inicio de sesión

**Como** usuario registrado, **quiero** iniciar sesión con correo y contraseña, **para** obtener acceso a mis tareas.

```gherkin
Escenario: Credenciales válidas
  Dado que tengo una cuenta activa
  Cuando envío mi correo y contraseña correctos
  Entonces la API responde 200 OK
  Y recibo un access token JWT firmado con expiración

Escenario: Credenciales inválidas
  Dado que tengo una cuenta registrada
  Cuando envío una contraseña incorrecta
  Entonces la API responde 401 Unauthorized
  Y no se entrega un access token
```

## HU-SEG-03 · Acceso a las tareas propias

**Como** usuario autenticado, **quiero** ver y modificar únicamente mis tareas, **para** mantener privada mi información.

```gherkin
Escenario: Consultar tareas propias
  Dado que Ana y Luis tienen tareas registradas
  Y he iniciado sesión como Ana
  Cuando consulto GET /api/v1/tasks usando mi JWT
  Entonces recibo únicamente las tareas pertenecientes a Ana

Escenario: Intentar modificar una tarea ajena
  Dado que la tarea 25 pertenece a Luis
  Y he iniciado sesión como Ana
  Cuando intento modificar PUT /api/v1/tasks/25
  Entonces el servidor rechaza la operación
  Y la tarea de Luis permanece sin cambios
```

> Para ocultar la existencia de registros de otros usuarios, en operaciones sobre una tarea por identificador se utilizará **404 Not Found** cuando la tarea no exista o no pertenezca al usuario autenticado. Deben mantener este criterio consistentemente en consulta, edición y eliminación.

## HU-SEG-04 · Protección de recursos

**Como** responsable del sistema, **quiero** que las APIs de tareas exijan un JWT válido, **para** impedir operaciones anónimas o con credenciales falsificadas.

```gherkin
Escenario: Petición sin token
  Cuando consulto GET /api/v1/tasks sin Authorization
  Entonces la API responde 401 Unauthorized

Escenario: Token manipulado o expirado
  Dado que el token fue alterado o expiró
  Cuando consulto GET /api/v1/tasks con ese token
  Entonces la API responde 401 Unauthorized
```

## HU-SEG-05 · Recurso administrativo

**Como** administrador, **quiero** consultar un resumen general no sensible del sistema, **para** conocer su utilización sin exponer las tareas privadas de los usuarios.

```gherkin
Escenario: Acceso administrativo autorizado
  Dado que mi cuenta tiene rol ADMIN
  Cuando consulto GET /api/v1/admin/summary
  Entonces la API responde 200 OK con métricas agregadas

Escenario: Usuario sin privilegios
  Dado que tengo un JWT válido con rol USER
  Cuando consulto GET /api/v1/admin/summary
  Entonces la API responde 403 Forbidden
```

## HU-SEG-06 · Sesión en React

**Como** usuario autenticado, **quiero** acceder a mis tareas mientras dure mi sesión y cerrar sesión cuando termine, **para** controlar el uso de mi cuenta.

```gherkin
Escenario: Cierre de sesión
  Dado que he iniciado sesión
  Cuando presiono "Cerrar sesión"
  Entonces se elimina el token del estado de la aplicación
  Y se ocultan mis tareas
  Y se muestra la pantalla de inicio de sesión
```

> **Nota:** en esta versión el cierre de sesión es **del lado del cliente**. No revoca anticipadamente un JWT ya emitido; el token continuará siendo válido hasta su expiración si alguien lo conserva. Explicar esta limitación en el informe.

---

# 4. Contrato REST requerido

## 4.1 Autenticación y perfil

| Método | Endpoint | Acceso | Resultado principal |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Público | `201 Created` |
| `POST` | `/api/v1/auth/login` | Público | `200 OK` y JWT |
| `GET` | `/api/v1/users/me` | Autenticado | `200 OK` con perfil propio |
| `GET` | `/api/v1/admin/summary` | `ADMIN` | `200 OK` |

### Ejemplo · Registro

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "name": "Ana Pérez",
  "email": "ana@example.com",
  "password": "ContrasenaDemo123!"
}
```

Respuesta representativa (sin secretos):

```json
{
  "id": 1,
  "name": "Ana Pérez",
  "email": "ana@example.com",
  "role": "USER"
}
```

### Ejemplo · Inicio de sesión

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "ana@example.com",
  "password": "ContrasenaDemo123!"
}
```

Respuesta conceptual:

```json
{
  "tokenType": "Bearer",
  "accessToken": "<JWT_FIRMADO>",
  "expiresIn": 1800
}
```

**El valor `<JWT_FIRMADO>` es únicamente un marcador ilustrativo. No se debe incluir en el repositorio ningún token real.**

## 4.2 Recursos de tareas existentes

**Mantener las rutas existentes** del laboratorio base e implementar control de acceso:

| Método | Endpoint | Acceso | Comportamiento |
|---|---|---|---|
| `GET` | `/api/v1/tasks` | Autenticado | Devuelve **solo mis tareas**. |
| `GET` | `/api/v1/tasks/{id}` | Autenticado | Devuelve mi tarea; si es ajena o no existe: `404`. |
| `POST` | `/api/v1/tasks` | Autenticado | Crea una tarea asociada a mi usuario. |
| `PUT` | `/api/v1/tasks/{id}` | Autenticado | Modifica exclusivamente mi tarea. |
| `DELETE` | `/api/v1/tasks/{id}` | Autenticado | Elimina exclusivamente mi tarea. |

Si existe un endpoint de calendario o búsqueda por fechas, debe retornar únicamente tareas del usuario autenticado.

### Ejemplo de petición protegida

```http
GET /api/v1/tasks
Authorization: Bearer <JWT_FIRMADO>
```

### Política de errores

| Situación | Código |
|---|---|
| Datos inválidos en registro o actualización | `400 Bad Request` |
| Registro con correo ya existente | `409 Conflict` |
| Login inválido | `401 Unauthorized` |
| Falta de token, token expirado o firma inválida | `401 Unauthorized` |
| JWT válido sin rol requerido para recurso administrativo | `403 Forbidden` |
| Tarea inexistente o perteneciente a otro usuario | `404 Not Found` |

---