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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorización por roles (HU-SEG-05) con la aplicación completa y PostgreSQL real.
 * El ADMIN se crea como en desarrollo local: registro público (USER) + UPDATE en la BD + nuevo login.
 * Cubre PT-14 y PT-15 y la diferencia entre 401, 403 y 404 (LAB 7 · Parte 5.7).
 */
class AdminIntegrationTest extends AbstractIntegrationTest {

    private static final String SUMMARY_URL = "/api/v1/admin/summary";
    private static final String PASSWORD = "ContrasenaDemo123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM users");
        register("Ana Pérez", "ana@example.com");
        register("Admin Local", "admin@example.com");
        // Única forma de obtener ADMIN: cambiar el rol directamente en la BD (nunca por la API pública)
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", "admin@example.com");

        userToken = login("ana@example.com");
        adminToken = login("admin@example.com"); // El rol viaja en el JWT: el ADMIN debe iniciar sesión después del cambio

        createTask(userToken, "Entregar laboratorio privado");
    }

    // ---------- Helpers ----------

    private void register(String name, String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "email", email, "password", PASSWORD))))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asString();
    }

    private void createTask(String token, String title) throws Exception {
        String body = """
                {"title": "%s", "priority": "HIGH", "dueDate": "%s"}
                """.formatted(title, LocalDate.now().plusDays(1));
        mockMvc.perform(post("/api/v1/tasks").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    // ---------- PT-14 / PT-15 ----------

    @Test
    @DisplayName("PT-15 · ADMIN accede a /admin/summary → 200 con métricas agregadas")
    void summary_shouldReturn200ForAdmin() throws Exception {
        mockMvc.perform(get(SUMMARY_URL).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.totalAdmins").value(1))
                .andExpect(jsonPath("$.totalTasks").value(1))
                .andExpect(jsonPath("$.tasksByStatus.PENDING").value(1))
                .andExpect(jsonPath("$.tasksByStatus.IN_PROGRESS").value(0))
                .andExpect(jsonPath("$.tasksByStatus.COMPLETED").value(0));
    }

    @Test
    @DisplayName("PT-15 · El resumen no expone datos privados (correos ni títulos)")
    void summary_shouldNotExposePrivateData() throws Exception {
        mockMvc.perform(get(SUMMARY_URL).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("ana@example.com"))))
                .andExpect(content().string(not(containsString("Entregar laboratorio privado"))));
    }

    @Test
    @DisplayName("PT-14 · USER accede a /admin/summary → 403")
    void summary_shouldReturn403ForUser() throws Exception {
        mockMvc.perform(get(SUMMARY_URL).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: insufficient role"));
    }

    @Test
    @DisplayName("Un USER promovido sigue siendo USER hasta que vuelve a iniciar sesión (el rol viaja en el JWT)")
    void summary_shouldReturn403WithTokenIssuedBeforePromotion() throws Exception {
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", "ana@example.com");

        mockMvc.perform(get(SUMMARY_URL).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(SUMMARY_URL).header("Authorization", "Bearer " + login("ana@example.com")))
                .andExpect(status().isOk());
    }

    // ---------- Diferencia 401 / 403 / 404 (Parte 5.7) ----------

    @Test
    @DisplayName("401 · /admin/summary sin token")
    void summary_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("404 · El ADMIN tampoco puede leer tareas de otros usuarios")
    void admin_shouldNotAccessTasksOfOtherUsers() throws Exception {
        Long anaTaskId = jdbcTemplate.queryForObject(
                "SELECT t.id FROM tasks t JOIN users u ON u.id = t.user_id WHERE u.email = ?", Long.class, "ana@example.com");

        mockMvc.perform(get("/api/v1/tasks").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/tasks/{id}", anaTaskId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
