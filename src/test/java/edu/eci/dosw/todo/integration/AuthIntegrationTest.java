package edu.eci.dosw.todo.integration;

import edu.eci.dosw.todo.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración del registro (HU-SEG-01) contra PostgreSQL real.
 * Cubre PT-01 (registro válido), PT-02 (correo duplicado) y PT-03 (hash de contraseña).
 */
class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String PASSWORD = "ContrasenaDemo123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM users");
    }

    private ResultActions register(String name, String email, String password) throws Exception {
        String body = """
                {"name": "%s", "email": "%s", "password": "%s"}
                """.formatted(name, email, password);
        return mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int countUsersWithEmail(String email) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
    }

    @Test
    @DisplayName("PT-01 · Registrar usuario válido → 201 con rol USER")
    void register_shouldCreateUserWithUserRole() throws Exception {
        register("Ana Pérez", "ana@example.com", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Ana Pérez"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String role = jdbcTemplate.queryForObject(
                "SELECT role FROM users WHERE email = ?", String.class, "ana@example.com");
        assertThat(role).isEqualTo("USER");
    }

    @Test
    @DisplayName("PT-01 · El registro público no puede elegir el rol ADMIN")
    void register_shouldNotAllowChoosingAdminRole() throws Exception {
        String body = """
                {"name": "Eva", "email": "eva@example.com", "password": "%s", "role": "ADMIN"}
                """.formatted(PASSWORD);

        mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));

        String role = jdbcTemplate.queryForObject(
                "SELECT role FROM users WHERE email = ?", String.class, "eva@example.com");
        assertThat(role).isEqualTo("USER");
    }

    @Test
    @DisplayName("PT-02 · Correo duplicado → 409 sin crear un segundo usuario")
    void register_shouldNotCreateDuplicateEmail() throws Exception {
        register("Ana Pérez", "ana@example.com", PASSWORD).andExpect(status().isCreated());

        register("Otra Ana", "ana@example.com", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(countUsersWithEmail("ana@example.com")).isEqualTo(1);
    }

    @Test
    @DisplayName("PT-02 · Correo equivalente (mayúsculas) también es duplicado")
    void register_shouldNotCreateDuplicateWithEquivalentEmail() throws Exception {
        register("Ana Pérez", "ana@example.com", PASSWORD).andExpect(status().isCreated());

        register("Ana Pérez", "ANA@Example.COM", PASSWORD).andExpect(status().isConflict());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("PT-03 · La contraseña se guarda como hash BCrypt, no en texto plano")
    void register_shouldStorePasswordAsBcryptHash() throws Exception {
        register("Ana Pérez", "ana@example.com", PASSWORD).andExpect(status().isCreated());

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?", String.class, "ana@example.com");

        assertThat(storedHash).isNotEqualTo(PASSWORD).doesNotContain(PASSWORD);
        assertThat(storedHash).startsWith("$2"); // Prefijo de BCrypt
        assertThat(passwordEncoder.matches(PASSWORD, storedHash)).isTrue();
    }

    @Test
    @DisplayName("Datos inválidos → 400 y no se crea el usuario")
    void register_shouldNotCreateUserWhenDataIsInvalid() throws Exception {
        register("Ana Pérez", "correo-invalido", PASSWORD).andExpect(status().isBadRequest());
        register("Ana Pérez", "ana@example.com", "corta").andExpect(status().isBadRequest());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
    }

    @Test
    @DisplayName("CORS · El preflight desde React (localhost:5173) pasa la cadena de seguridad")
    void register_shouldAllowCorsPreflightFromFrontend() throws Exception {
        mockMvc.perform(options(REGISTER_URL)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
