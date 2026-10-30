# Informe · Seguridad con Spring Security y JWT (Lab 7)

**Escuela Colombiana de Ingeniería Julio Garavito** · Desarrollo y Operaciones de Software (DOSW)
**Caso de estudio:** App ToDo · Gestión de tareas

Este informe responde las preguntas de comprensión de la sección 9 de [LAB7.md](lab7/LAB7.md) con base en la implementación entregada. El detalle de diseño, el contrato de endpoints, las decisiones de seguridad y las evidencias están en el [README](../README.md).

---

## 1. ¿Cuál es la diferencia entre autenticación y autorización? Dé un ejemplo real de ToDo.

**Autenticación** es comprobar *quién* hace la petición. **Autorización** es decidir *qué* puede hacer esa identidad ya comprobada.

En ToDo:

- **Autenticación:** `POST /api/v1/auth/login` verifica correo y contraseña (`AuthServiceImpl.login` con `PasswordEncoder.matches`) y entrega un JWT. En cada petición siguiente, la cadena de Spring Security (`SecurityConfig` + `JwtDecoder` de `JwtConfig`) verifica la firma, la expiración y el emisor del token. Si el token no es válido, la petición no está autenticada y responde `401`.
- **Autorización:** con el usuario ya identificado se aplican dos reglas:
  - **Por rol:** `/api/v1/admin/summary` exige `hasRole('ADMIN')`. Ana tiene un token válido, pero su rol es `USER`, así que recibe `403`.
  - **Por identidad (propiedad):** `TaskServiceImpl` solo opera sobre tareas cuyo `user_id` coincide con el `sub` del token. Luis está autenticado, pero no está autorizado a ver ni a modificar la tarea de Ana: recibe `404`.

## 2. Explique cómo se genera un JWT, qué contienen sus partes y cómo verifica Spring Boot que no fue manipulado.

**Generación** (`TokenService.generateToken`): después de un login válido se arma un `JwtClaimsSet` y se firma con `NimbusJwtEncoder` usando HMAC-SHA256 (HS256) y la llave `JWT_SECRET`, configurada en `JwtConfig`.

Un JWT tiene tres partes en Base64URL separadas por puntos, `header.payload.firma`:

| Parte | Contenido en ToDo |
|---|---|
| Header | `{"alg": "HS256"}`: algoritmo de firma |
| Payload (claims) | `sub` (id del usuario), `iss` (`todo-api`), `iat` (emisión), `exp` (emisión + 30 min), `role` (`USER`/`ADMIN`) y `name`. No incluye correo, contraseña ni hash. |
| Firma | `HMAC-SHA256(header + "." + payload, JWT_SECRET)` |

**Verificación** (`JwtConfig.jwtDecoder`): en cada petición protegida, `NimbusJwtDecoder` **recalcula la firma** del header y el payload recibidos con la misma llave y la compara con la firma del token. Si se cambió un solo carácter del header, del payload o de la firma, no coinciden y el token se rechaza. Después se aplican los validadores:

- `exp`/`nbf`: el token no está vencido;
- `iss`: el emisor es `todo-api`;
- un validador propio: `sub` numérico y `role` dentro de {`USER`, `ADMIN`}.

Encoder y decoder se construyen con la misma `SecretKey` en `JwtConfig`, así que usan la misma configuración criptográfica.

## 3. ¿Por qué decodificar el payload de un JWT no equivale a validar su autenticidad?

Porque el payload solo está **codificado** en Base64URL, no cifrado ni protegido por sí mismo: cualquiera puede decodificarlo, cambiarlo y volver a codificarlo. Lo que garantiza que no fue alterado es la **firma**, y solo quien tiene la llave puede producir una firma válida.

La prueba `SecurityIntegrationTest.getTasks_shouldReturn401WhenPayloadIsModified` lo demuestra: toma un token válido, decodifica el payload, cambia `"role":"USER"` por `"role":"ADMIN"`, lo vuelve a codificar y conserva la firma original. El resultado se "ve" correcto al decodificarlo, pero el back-end responde `401`, porque la firma ya no corresponde al contenido. Por eso el back-end nunca confía en el contenido del token sin pasar por `JwtDecoder`.

## 4. ¿Qué sucede exactamente cuando un token vence y el usuario intenta consultar sus tareas?

1. React llama `GET /api/v1/tasks` con `Authorization: Bearer <token vencido>` (`taskApi.js`).
2. En el back-end, el filtro de Resource Server pasa el token a `JwtDecoder`. La firma es válida, pero el validador de tiempo detecta que `exp` ya pasó y la autenticación falla.
3. La petición **no llega** a `TaskController`. `JsonAuthenticationEntryPoint` responde `401` con `{"status": 401, "message": "Invalid or expired token"}` y la cabecera `WWW-Authenticate: Bearer`.
4. En React, `taskApi.js` detecta el `401` y llama a `notifyUnauthorized()` (`api/session.js`), que borra el token de memoria y avisa a `AuthContext`.
5. `AuthContext` limpia el usuario y muestra la pantalla de inicio de sesión con el aviso "Tu sesión expiró, inicia sesión nuevamente". Las tareas desaparecen porque `TasksPage` se desmonta.

No hay refresh tokens: para continuar, el usuario debe iniciar sesión otra vez. Lo comprueban `SecurityIntegrationTest.getTasks_shouldReturn401WhenTokenIsExpired` (back-end) y `SessionExpiration.test.jsx` (PT-17, front-end).

## 5. ¿Por qué no se deben almacenar las contraseñas en texto plano? ¿Cómo verifican la contraseña durante el login?

Si la base de datos se filtra (un respaldo, un acceso indebido, un error de configuración), las contraseñas en texto plano quedan expuestas de inmediato. Como muchas personas reutilizan contraseñas, el daño alcanza a otros servicios. Además, ni los administradores del sistema deberían poder conocerlas.

En ToDo solo se guarda `password_hash`, calculado con **BCrypt** (`BCryptPasswordEncoder`, bean de `SecurityConfig`):

- BCrypt agrega una **sal aleatoria** por contraseña: dos usuarios con la misma contraseña tienen hashes distintos.
- Es **lento a propósito** (factor de costo 10), lo que encarece los ataques de fuerza bruta.

**Verificación en el login** (`AuthServiceImpl.login`):

1. Se normaliza el correo y se busca el usuario.
2. `passwordEncoder.matches(contraseñaIngresada, hashGuardado)` toma la sal y el costo que vienen dentro del hash, calcula el hash de la contraseña ingresada y lo compara. La contraseña nunca se "descifra".
3. Si el usuario no existe o no coincide, se responde el **mismo** `401` con el mismo mensaje. Si el correo no existe, también se compara contra un hash de relleno, para que el tiempo de respuesta no revele qué cuentas existen.

Lo comprueba `AuthIntegrationTest.register_shouldStorePasswordAsBcryptHash` (PT-03): el valor en la BD empieza por `$2`, no contiene la contraseña y `matches` la reconoce.

## 6. ¿Qué diferencia existe entre responder 401 Unauthorized y 403 Forbidden? Dé ejemplos de los endpoints implementados.

- **401 Unauthorized:** el servidor **no sabe quién eres** o no puede confiar en tu identidad, porque no hay token o el token no es válido. La solución es autenticarse (de nuevo).
- **403 Forbidden:** el servidor **sí sabe quién eres** (token válido), pero tu identidad no tiene permiso para ese recurso. Volver a autenticarse con la misma cuenta no cambia nada.

| Petición | Respuesta | Quién la produce |
|---|---|---|
| `GET /api/v1/tasks` sin `Authorization` | `401` "Authentication is required" | `JsonAuthenticationEntryPoint` |
| `GET /api/v1/tasks` con token alterado, vencido o firmado con otra llave | `401` "Invalid or expired token" | `JsonAuthenticationEntryPoint` |
| `POST /api/v1/auth/login` con contraseña incorrecta | `401` "Invalid email or password" | `GlobalExceptionHandler` |
| `GET /api/v1/admin/summary` con token válido de un `USER` | `403` "Access denied: insufficient role" | `JsonAccessDeniedHandler` |
| `GET /api/v1/admin/summary` con token válido de un `ADMIN` | `200` | `AdminController` |

Un tercer caso completa el cuadro: una tarea **ajena** responde `404`, no `403`, para no revelar que existe (ver pregunta 7). `AdminIntegrationTest` y `SecurityIntegrationTest` verifican estas diferencias.

## 7. ¿Cómo impidieron que un usuario editara una tarea de otro, incluso con un JWT válido?

La restricción está en el back-end, en tres capas:

1. **La identidad sale del token, no del cliente.** `TaskController` obtiene el `userId` del claim `sub` del JWT ya validado (`@AuthenticationPrincipal Jwt`). Un `userId` u `owner` enviado en el body se ignora (`createTask_shouldIgnoreOwnerSentByClient`).
2. **Las consultas filtran por propietario.** `TaskRepository` solo ofrece consultas con dueño: `findByIdAndOwnerId`, `existsByIdAndOwnerId`, `findAllByOwnerId` y `findByOwnerIdAndDueDateBetween`. Para editar, `TaskServiceImpl.update` busca con `findByIdAndOwnerId(id, userId)`. Si la tarea es de otro usuario, la consulta no la encuentra y se lanza `TaskNotFoundException`, **antes** de llegar a `save`.
3. **Respuesta `404`.** La tarea ajena se trata igual que una inexistente, así que no se revela que existe.

Además, la base de datos garantiza con `user_id NOT NULL` y la FK `fk_tasks_user` que toda tarea tiene un propietario real.

`TaskOwnershipIntegrationTest.updateTask_shouldReturn404AndKeepTaskOfAnotherUserUnchanged` (PT-11) lo prueba con la aplicación completa: Luis, con un JWT válido, intenta `PUT /api/v1/tasks/{id}` sobre la tarea de Ana. Recibe `404` y la fila en PostgreSQL queda idéntica. Lo mismo pasa con `GET` (PT-10) y `DELETE` (PT-12).

## 8. ¿Por qué ocultar botones en React no es suficiente para proteger los datos?

Porque el front-end se ejecuta en el navegador del usuario, que lo controla por completo. Cualquiera puede ver el código, usar las herramientas de desarrollador o, sin React, llamar la API directamente con `curl` o Postman, inventar IDs de tareas y enviar el token que obtuvo con su propia cuenta. Ocultar un botón solo cambia lo que se ve, no lo que el servidor acepta.

Por eso en ToDo todas las reglas viven en el back-end: la cadena de Spring Security exige el token y el rol, y el servicio y el repositorio filtran por propietario. Las evidencias de HU-SEG-03 muestran a Luis atacando la API con `curl` (sin React) y recibiendo `404` en `GET`, `PUT` y `DELETE` sobre la tarea de Ana.

## 9. ¿Qué ventajas y limitaciones tuvo conservar el JWT en memoria en el frontend?

En ToDo el token vive en una variable del módulo `src/api/session.js`; nunca se guarda en `localStorage`, `sessionStorage` ni cookies.

**Ventajas:**

- Un script malicioso inyectado (XSS) no puede leer un token persistido en el almacenamiento del navegador, porque no existe.
- No quedan tokens olvidados en el navegador después de cerrarlo.
- El cierre de sesión es simple y completo del lado del cliente: borrar la variable y el estado de React.
- Implementación sencilla, sin cookies ni protección CSRF adicional.

**Limitaciones:**

- **Al recargar la página (F5) o abrir otra pestaña se pierde la sesión** y hay que volver a iniciar sesión (evidencia `hu06_recarga_login.png`).
- Sin refresh tokens, al vencer los 30 minutos el usuario tiene que volver a autenticarse aunque esté trabajando.
- Mientras la página está abierta, un XSS todavía podría usar el token desde el propio código en ejecución: la memoria reduce la exposición, pero no reemplaza prevenir el XSS.

## 10. ¿Qué limitación tiene el cierre de sesión sin revocación de tokens? ¿Qué solución propondría para un sistema real?

En ToDo, "Cerrar sesión" es **del lado del cliente**: borra el token de memoria y el estado de React. El servidor no guarda sesiones (es stateless) y no lleva registro de tokens emitidos, así que **el JWT sigue siendo válido hasta su `exp`** (máximo 30 minutos). Si alguien copió el token antes del cierre de sesión, podría seguir usándolo durante ese tiempo. Por la misma razón, si se cambia el rol de un usuario en la BD, su token anterior conserva el rol viejo hasta vencer (`AdminIntegrationTest.summary_shouldReturn403WithTokenIssuedBeforePromotion`).

Para un sistema real se propondría:

- **Access tokens de vida corta** (5 a 15 minutos) más **refresh tokens** guardados en una cookie `HttpOnly`, `Secure` y `SameSite`, que el servidor pueda **revocar** al cerrar sesión.
- **Lista de revocación** (denylist) de tokens por su identificador (`jti`) hasta su expiración, por ejemplo en Redis, consultada por el `JwtDecoder`.
- **Versión de sesión por usuario** (`token_version` en `users`) incluida en el JWT: al cerrar sesión en todos los dispositivos, cambiar la contraseña o cambiar el rol, se incrementa y los tokens anteriores dejan de ser aceptados.

## 11. ¿Cuáles son las consecuencias de almacenar una llave de firma JWT dentro de GitHub?

Con HS256, la misma llave sirve para **firmar** y para **verificar**. Si la llave se publica:

- Cualquiera puede **fabricar tokens válidos**: con cualquier `sub` (hacerse pasar por cualquier usuario y leer o borrar sus tareas) y con `role: ADMIN` (acceder a `/admin/summary`). El back-end no puede distinguir esos tokens de los legítimos.
- El historial de Git es permanente y suele estar clonado o en caché: borrar el archivo en un commit posterior **no** la elimina. La única solución es **rotar** la llave, lo que invalida todas las sesiones activas.
- Todas las protecciones de este laboratorio (propiedad de tareas, rol ADMIN) quedan anuladas, porque todas dependen de confiar en el token.

**Medidas aplicadas en ToDo:**

- La llave se lee de la variable de entorno `JWT_SECRET` (`app.jwt.secret: ${JWT_SECRET}`, **sin valor por defecto**): si falta, la aplicación no arranca.
- Para desarrollo se usa un `.env` local excluido por `.gitignore`. El repositorio solo tiene `.env.example`, sin valores.
- `JwtConfig` rechaza llaves de menos de 32 bytes.
- Las pruebas generan una llave aleatoria en cada ejecución (`JwtTestSupport`), así que tampoco hay llaves de prueba versionadas.

## 12. ¿Qué pruebas del equipo demuestran la seguridad del CRUD y no solamente su funcionamiento correcto?

Las pruebas del Lab 6 demostraban que el CRUD *funciona*. Las del Lab 7 demuestran que **resiste usos indebidos**: casos negativos contra la aplicación completa, PostgreSQL real (Testcontainers) y tokens reales. La matriz completa PT-01..PT-18 está en el [README](../README.md#112-matriz-de-pruebas-pt).

| Amenaza | Prueba | Resultado verificado |
|---|---|---|
| Acceso anónimo al CRUD y al calendario | `SecurityIntegrationTest.taskEndpoints_shouldReturn401WithoutToken` (PT-06) | `401` en `GET`, `POST`, `PUT`, `DELETE` y `?from&to` |
| Token con la firma modificada | `getTasks_shouldReturn401WhenSignatureIsModified` (PT-08) | `401` |
| Escalada de rol editando el payload | `getTasks_shouldReturn401WhenPayloadIsModified` (PT-08) | `401` |
| Token firmado con otra llave, otro emisor, rol inventado, `sub` inválido | `SecurityIntegrationTest` | `401` |
| Token vencido | `getTasks_shouldReturn401WhenTokenIsExpired` (PT-09) | `401` |
| Leer una tarea ajena | `TaskOwnershipIntegrationTest.getTaskById_shouldReturn404ForTaskOfAnotherUser` (PT-10) | `404` sin el título en la respuesta |
| Modificar una tarea ajena | `updateTask_shouldReturn404AndKeepTaskOfAnotherUserUnchanged` (PT-11) | `404` y la fila en la BD idéntica |
| Eliminar una tarea ajena | `deleteTask_shouldReturn404AndKeepTaskOfAnotherUser` (PT-12) | `404` y la fila sigue en la BD |
| Suplantar al propietario desde el body | `createTask_shouldIgnoreOwnerSentByClient` (PT-13) | `user_id` = usuario del token |
| `USER` en el recurso administrativo | `AdminIntegrationTest.summary_shouldReturn403ForUser` (PT-14) | `403` |
| Un `ADMIN` leyendo tareas privadas | `AdminIntegrationTest.admin_shouldNotAccessTasksOfOtherUsers` | `404` |
| Elegir el rol `ADMIN` en el registro público | `AuthIntegrationTest.register_shouldNotAllowChoosingAdminRole` | Se crea como `USER` |
| Contraseñas en texto plano | `AuthIntegrationTest.register_shouldStorePasswordAsBcryptHash` (PT-03) | Hash BCrypt en la BD |
| Enumeración de cuentas en el login | `AuthServiceTest.login_shouldNotRevealWhetherEmailExists` (PT-05) | Mismo `401` y mismo mensaje |
| Tareas visibles después del cierre de sesión | `App.test.jsx` (PT-16) | Sin token ni tareas en la UI |
| Uso de un token rechazado en React | `SessionExpiration.test.jsx` (PT-17) | Vuelve al inicio de sesión |
| Pérdida de datos al migrar | `MigrationScriptsTest` | Ninguna tarea borrada; todas con propietario |

**Resultados finales:** ver la sección [Pruebas y cobertura](../README.md#11-pruebas-y-cobertura) del README. Como indica el laboratorio, la cobertura de JaCoCo complementa, pero no reemplaza, estos escenarios de seguridad: una línea ejecutada no prueba que se haya rechazado un ataque.
