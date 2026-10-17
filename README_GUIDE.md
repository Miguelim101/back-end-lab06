# GUÍA PARA DESARROLLAR UNA NUEVA FUNCIONALIDAD

> **Importante:** Esta guía está basada en la estructura de los proyectos `back-end-lab06` (Spring Boot + JPA + PostgreSQL) y `front-end-lab06` (React + Vite). Se deben seguir las convenciones que ya existen en el código (paquetes `edu.eci.dosw.todo`, capas entity/dto/repository/service/controller, componentes en `features/tasks`, API en `api/taskApi.js`).

---

## 1. OBJETIVO DE ESTA GUÍA

La actividad consiste en implementar una nueva funcionalidad sobre la aplicación ToDo existente, aplicando el proceso completo de ingeniería de software (análisis, diseño, implementación, integración, pruebas, documentación y entrega). Esta guía detalla **qué se debe tener preparado ANTES** del inicio y **qué se debe hacer DURANTE y DESPUÉS** para cumplir todos los puntos del requerimiento.

---

## 2. SECCIÓN A: PREPARACIÓN OBLIGATORIA (ANTES)

> **Plazos obligatorios:**
> * Entrega inicial en Teams **antes** del inicio.
> * Publicación del enunciado **después** de la entrega inicial.


Se deben cumplir **TODOS** los ítems de esta sección en la fase previa.

### A1. Verificar el estado del repositorio

* [X] Repositorio privado de GitHub con versión **completa y funcional**.
* [X] Backend en `back-end-lab06/` y frontend en `front-end-lab06/`.
* [X] **Commit inicial estable**. Guardar **captura de pantalla del historial de commits**.
* [X] `.gitignore` correcto (no subir `target/`, `node_modules/`, etc.).

### A2. Backend: tenerlo completamente preparado

* [X] **Compila sin errores:** `mvn clean compile` en `back-end-lab06/`.
* [X] **Pruebas existentes en verde:** `mvn test`.
* [X] **PostgreSQL con Docker:** contenedor `todo-postgres` corriendo. Verificar con `docker ps`. Arrancar con `docker start todo-postgres` si está detenido.
* [X] **application.properties correcto:** Revisar variables `DB_URL`, `DB_USER`, `DB_PASSWORD`. Entender `ddl-auto=validate`.
* [X] **CRUD completo funcionando:** Probar `GET`, `GET/{id}`, `POST`, `PUT/{id}`, `DELETE/{id}` (Postman/PowerShell).
* [X] **Validaciones y manejo de errores** OK.
* [X] **CORS configurado:** `WebConfig.java` permite `http://localhost:5173` y métodos necesarios (añadir `PATCH` si se usará).

### A3. Frontend: tenerlo completamente preparado

* [X] `npm install` ejecutado (o `node_modules` estable).
* [X] `npm run dev` funciona en `.
* [X] Comunicación backend-frontend verificada end-to-end.
* [ ] Estructura en `features/tasks/` y `api/taskApi.js` correcta.
* [X] Sin warnings críticos en consola.

### A4. Entrega inicial obligatoria

* [X] Invitar a `rodrigogualtero92@gmail.com` al repositorio privado (permisos suficientes).
* [X] Push de versión inicial **antes** del inicio.
* [X] Captura de historial de commits guardada.
* [X] Entregar en Teams: enlace del repo, captura y confirmación de invitación **antes** de iniciar (repositorios con commits posteriores a la entrega inicial carecen de validez en esta fase).

### A5. Verificación integral previa

* [X] Flujo completo: crear → consultar → editar → eliminar **desde React hasta PostgreSQL**.
* [X] Arranque simultáneo backend y frontend OK.

---

## 3. SECCIÓN B: AL RECIBIR EL ENUNCIADO (DESPUÉS DEL INICIO)

* [X] Leer enunciado completo (requerimiento + mockup).
* [X] Identificar entidades, atributos, reglas, estados, validaciones, flujos.

---

## 4. SECCIÓN C: DESARROLLO (PASO A PASO)

### C1. Análisis y diseño

* [ ] **HU + Criterios:** Definir Épica → Feature → HUs (`Como... quiero... para...`) con criterios Given/When/Then.
* [ ] **Casos borde:** Identificar valores nulos, duplicados, estados inválidos.
* [ ] **Jira:** Crear épica, feature, HUs con criterios, descomponer en tareas.
* [ ] **Diagramas:** Actualizar Diagrama de Clases y Diagramas C4 (Context/Container/Component) en `docs/`.
* [ ] **Modelo de datos:** Identificar nueva entidad/enum/campos/relaciones. Respetar convenciones JPA. Considerar impacto con `ddl-auto=validate`.

### C2. Backend - Por capas (orden estricto)

* [ ] **Entidades/Enums (`entity/`)**: Crear/actualizar. Añadir getters/setters, equals/hashCode si aplica. Verificar arranque.
* [ ] **DTOs (`dto/`)**: Clases con getters/setters + validaciones (`@Valid`). **Nunca exponer entidades**.
* [ ] **Repository (`repository/`)**: Extender `JpaRepository`. Usar métodos derivados o `@Query` JPQL. Evitar SQL nativo salvo justificación.
* [ ] **Service (`service/`)**: Interfaz + Impl. Reglas de negocio aquí. Inyección por constructor. `@Transactional` (readOnly en lecturas). Excepciones propias en `exception/`. Mapper Entity→DTO.
* [ ] **Controller (`controller/`)**: Endpoints REST bajo `/api/v1/tasks`. Códigos HTTP correctos (200/201/204). `@Valid` en body.
* [ ] **GlobalExceptionHandler (`exception/`)**: Mapear nuevas excepciones a códigos HTTP (400/404/409/422).
* [ ] **Pruebas backend:** Service con Mockito (`@ExtendWith(MockitoExtension.class)`), Controller con `@WebMvcTest` + `@MockitoBean` (Spring Boot 4), Repository con `@DataJpaTest` si aplica. **`mvn test` 100% verde**.
* [ ] **CORS (`config/WebConfig.java`)**: Permitir `http://localhost:5173`. Añadir `PATCH` si se usará.

**Checkpoint:** Backend arranca, endpoints OK, pruebas verdes, integrado con BD.

### C3. Frontend - React + Vite

* [ ] **API (`api/taskApi.js`)**: Añadir nuevos endpoints. Mantener patrón `request`. Para 204 devuelve `null`.
* [ ] **Constantes/estados**: Ajustar `NEXT`/`NEXT_LABEL` si cambian estados. Coherencia con enums backend.
* [ ] **Componentes (`features/tasks/components/`)**: Ajustar `TaskForm.jsx`, `TaskItem.jsx`, `TaskList.jsx`. Crear nuevos solo si es necesario.
* [ ] **Hook (`features/tasks/hooks/useTasks.js`)**: Nuevas operaciones con patrón `run(fn)` que recarga lista.
* [ ] **Página (`features/tasks/pages/TasksPage.jsx`)**: Integrar según mockup.
* [ ] **Estilos (`index.css`)**: Usar existentes, mínimos si requiere.
* [ ] **Tests (`npm test`)**: Verdes si existen.

**Checkpoint:** UI integrada con backend.

### C4. Integración completa

* [ ] Levantar backend (`mvn spring-boot:run`) y frontend (`npm run dev`).
* [ ] Probar **todos los flujos desde React hasta PostgreSQL**.
* [ ] Verificar persistencia tras recarga.
* [ ] Revisar consolas/logs sin errores.

---

## 5. SECCIÓN D: DOCUMENTACIÓN Y ENTREGA FINAL

### D1. README principal (obligatorio)

Actualizar README con al menos:

* [ ] Descripción de la nueva funcionalidad.
* [ ] Historias de Usuario + Criterios de Aceptación.
* [ ] Diagramas (C4 y Clases) en `docs/` y referenciados.
* [ ] Modelo de Datos (cambios realizados).
* [ ] Arquitectura (backend/frontend).
* [ ] Instrucciones de ejecución (prerrequisitos, Docker, backend, frontend, variables de entorno).
* [ ] Endpoints relevantes.
* [ ] Pruebas (`mvn test`, `npm test`) y estado.
* [ ] Evidencias si aplica.

### D2. Video de demostración (obligatorio)

* [ ] Demostrar **funcionamiento desde React hasta PostgreSQL** (localmente). **NO solo Postman**.
* [ ] Mostrar flujo completo + casos relevantes.
* [ ] Audio claro y demostrativo.

### D3. Entrega final

* [ ] Todos los cambios subidos al repositorio.
* [ ] **Historial de commits completo y coherente** (evidencia de trabajo autónomo).
* [ ] README, diagramas y código en versión final.

---

## 6. SECCIÓN E: RESTRICCIONES (OBLIGATORIO CUMPLIR)

* [ ] **Individualidad:** Sin ayuda de terceros.
* [ ] **Sin IA:** Prohibido el uso de asistentes generadores de código.
* [ ] **Sin consultas externas:** Solo material previamente incorporado en el README de apoyo autorizado.
* [ ] **Normativa:** Trabajo de autoría propia y auditable.

---

## 7. SECCIÓN F: CHECKLIST DE VALIDACIÓN "ANTES Y DESPUÉS"

### F1. ANTES (fase previa)

* [ ] Backend compila, tests verdes, PostgreSQL OK, CRUD verificado.
* [ ] Frontend OK, comunica con backend.
* [ ] Entrega inicial en Teams **antes** del inicio (enlace repo + captura commits + confirmación invitación).

### F2. DURANTE

* [ ] Análisis, HU+criterios, Jira, diagramas, modelo de datos.
* [ ] Backend: capas completas, validaciones, pruebas verdes (`mvn test`).
* [ ] Frontend: acorde a mockup, integrado.
* [ ] Integración **React → PostgreSQL** verificada.

### F3. DESPUÉS (entrega final)

* [ ] README principal actualizado.
* [ ] Video demuestra **React → PostgreSQL**.
* [ ] Repositorio con cambios + historial de commits completo y coherente.

### F4. SMOKE TEST FINAL

* [ ] `mvn clean test` en `back-end-lab06/` → **EXITOSA**.
* [ ] Backend y frontend levantan sin errores.
* [ ] Flujo completo funciona en navegador.
* [ ] Datos persisten en PostgreSQL.

---

## 8. NOTAS PRÁCTICAS

* **Convenciones:** Paquetes `edu.eci.dosw.todo`, DTOs con getters/setters (sin Lombok/records), `@Enumerated(EnumType.STRING)`, inyección por constructor, mapper Entity→DTO **siempre en Service** (nunca exponer entidades).
* **Spring Boot 4:** Usar `@MockitoBean` (no `@MockBean`) en tests con `@WebMvcTest`. Starters: `spring-boot-starter-webmvc` y `spring-boot-starter-webmvc-test`.
* **CORS:** Si se usa `PATCH`, añadirlo a `allowedMethods` en `WebConfig.java`.
* **ddl-auto=validate:** Cambios de esquema deben ser coherentes con la BD. Columnas `nullable=false` con datos existentes requieren precaución.
* **Frontend API:** `taskApi.js` devuelve `null` cuando `res.status === 204`. Ajustar si es necesario manejar cabeceras `Location`.