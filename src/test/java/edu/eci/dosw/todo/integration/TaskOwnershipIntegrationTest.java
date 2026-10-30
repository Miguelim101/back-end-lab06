package edu.eci.dosw.todo.integration;

import edu.eci.dosw.todo.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

/**
 * Aislamiento de tareas por propietario (HU-SEG-03) con la aplicación completa y PostgreSQL real.
 * Ana y Luis tienen tareas distintas; Luis intenta leer, modificar y eliminar la tarea de Ana.
 * Cubre PT-07, PT-10, PT-11, PT-12 y PT-13.
 */
class TaskOwnershipIntegrationTest extends AbstractIntegrationTest {

    private static final String TASKS_URL = "/api/v1/tasks";
    private static final String PASSWORD = "ContrasenaDemo123!";
    // Fechas relativas a hoy: al crear, dueDate debe ser hoy o futura (@FutureOrPresent)
    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String anaToken;
    private String luisToken;
    private long anaId;
    private long luisId;
    private long anaTaskId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM users"); // ON DELETE CASCADE también limpia tasks
        anaToken = registerAndLogin("Ana Pérez", "ana@example.com");
        luisToken = registerAndLogin("Luis Gómez", "luis@example.com");
        anaId = userId("ana@example.com");
        luisId = userId("luis@example.com");

        anaTaskId = createTask(anaToken, "Entregar laboratorio", TODAY.plusDays(1));
        createTask(anaToken, "Organizar apuntes", TODAY.plusDays(3));
        createTask(luisToken, "Tarea privada de Luis", TODAY.plusDays(2)); // misma semana que las de Ana
    }

    // ---------- Helpers ----------

    private String registerAndLogin(String name, String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asString();
    }

    private long userId(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private long createTask(String token, String title, LocalDate dueDate) throws Exception {
        String body = """
                {"title": "%s", "description": "Privada", "priority": "HIGH", "dueDate": "%s"}
                """.formatted(title, dueDate);
        String response = mockMvc.perform(post(TASKS_URL).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Map<String, Object> taskRow(long id) {
        return jdbcTemplate.queryForMap("SELECT title, status, priority, user_id FROM tasks WHERE id = ?", id);
    }

    // ---------- PT-07 · Solo las tareas propias ----------

    @Test
    @DisplayName("PT-07 · GET /tasks con token válido → 200 y solo las tareas propias")
    void getTasks_shouldReturnOnlyOwnTasks() throws Exception {
        mockMvc.perform(get(TASKS_URL).header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(content().string(containsString("Entregar laboratorio")))
                .andExpect(content().string(not(containsString("Tarea privada de Luis"))));

        mockMvc.perform(get(TASKS_URL).header("Authorization", "Bearer " + luisToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Tarea privada de Luis"));
    }

    @Test
    @DisplayName("PT-07 · El calendario (?from&to) solo devuelve tareas propias de la semana")
    void getTasksByRange_shouldReturnOnlyOwnTasks() throws Exception {
        mockMvc.perform(get(TASKS_URL).param("from", TODAY.toString()).param("to", TODAY.plusDays(6).toString())
                        .header("Authorization", "Bearer " + luisToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Tarea privada de Luis"));
    }

    // ---------- PT-10 / PT-11 / PT-12 · Tarea ajena ----------

    @Test
    @DisplayName("PT-10 · Luis consulta la tarea de Ana → 404 sin filtrar datos")
    void getTaskById_shouldReturn404ForTaskOfAnotherUser() throws Exception {
        mockMvc.perform(get(TASKS_URL + "/{id}", anaTaskId).header("Authorization", "Bearer " + luisToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(content().string(not(containsString("Entregar laboratorio"))));
    }

    @Test
    @DisplayName("PT-10 · Tarea ajena e inexistente responden igual (no se revela que existe)")
    void getTaskById_shouldRespondSameForForeignAndMissingTask() throws Exception {
        long missingId = anaTaskId + 1000;

        mockMvc.perform(get(TASKS_URL + "/{id}", missingId).header("Authorization", "Bearer " + luisToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(TASKS_URL + "/{id}", anaTaskId).header("Authorization", "Bearer " + luisToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PT-11 · Luis modifica la tarea de Ana → 404 y la BD no cambia")
    void updateTask_shouldReturn404AndKeepTaskOfAnotherUserUnchanged() throws Exception {
        Map<String, Object> before = taskRow(anaTaskId);
        String body = """
                {"title": "Hackeada por Luis", "status": "COMPLETED", "priority": "LOW"}
                """;

        mockMvc.perform(put(TASKS_URL + "/{id}", anaTaskId).header("Authorization", "Bearer " + luisToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());

        assertThat(taskRow(anaTaskId)).isEqualTo(before);
        assertThat(taskRow(anaTaskId).get("title")).isEqualTo("Entregar laboratorio");
    }

    @Test
    @DisplayName("PT-12 · Luis elimina la tarea de Ana → 404 y la tarea sigue en la BD")
    void deleteTask_shouldReturn404AndKeepTaskOfAnotherUser() throws Exception {
        mockMvc.perform(delete(TASKS_URL + "/{id}", anaTaskId).header("Authorization", "Bearer " + luisToken))
                .andExpect(status().isNotFound());

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tasks WHERE id = ?", Integer.class, anaTaskId);
        assertThat(count).isEqualTo(1);
    }

    // ---------- PT-13 · Propietario correcto ----------

    @Test
    @DisplayName("PT-13 · Crear tarea autenticada asigna como propietario al usuario del token")
    void createTask_shouldAssignAuthenticatedUserAsOwner() throws Exception {
        long id = createTask(luisToken, "Nueva de Luis", TODAY.plusDays(5));

        assertThat(((Number) taskRow(id).get("user_id")).longValue()).isEqualTo(luisId);
    }

    @Test
    @DisplayName("PT-13 · Un propietario enviado en el body se ignora")
    void createTask_shouldIgnoreOwnerSentByClient() throws Exception {
        String body = """
                {"title": "Intento de suplantación", "priority": "LOW", "userId": %d, "owner": {"id": %d}}
                """.formatted(anaId, anaId);

        String response = mockMvc.perform(post(TASKS_URL).header("Authorization", "Bearer " + luisToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        assertThat(((Number) taskRow(id).get("user_id")).longValue()).isEqualTo(luisId);
    }

    // ---------- El CRUD original sigue funcionando para el dueño ----------

    @Test
    @DisplayName("El propietario puede consultar, editar y eliminar su tarea")
    void owner_shouldManageOwnTask() throws Exception {
        String auth = "Bearer " + anaToken;
        String body = """
                {"title": "Entregar laboratorio", "status": "IN_PROGRESS", "priority": "HIGH", "dueDate": "%s"}
                """.formatted(TODAY.plusDays(1));

        mockMvc.perform(get(TASKS_URL + "/{id}", anaTaskId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Entregar laboratorio"));
        mockMvc.perform(put(TASKS_URL + "/{id}", anaTaskId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(delete(TASKS_URL + "/{id}", anaTaskId).header("Authorization", auth))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(TASKS_URL + "/{id}", anaTaskId).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("La respuesta de una tarea no expone datos del propietario")
    void getTask_shouldNotExposeOwnerData() throws Exception {
        mockMvc.perform(get(TASKS_URL + "/{id}", anaTaskId).header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist());
    }
}
