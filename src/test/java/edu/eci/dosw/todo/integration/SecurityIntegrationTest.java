package edu.eci.dosw.todo.integration;

import edu.eci.dosw.todo.config.JwtConfig;
import edu.eci.dosw.todo.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.function.Consumer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de la protección de recursos (HU-SEG-04) contra la aplicación completa.
 * Cubre PT-06 (sin token), PT-08 (token modificado) y PT-09 (token expirado), además de tokens
 * con otro emisor, firmados con otra llave o con claims incoherentes.
 */
class SecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String TASKS_URL = "/api/v1/tasks";
    private static final String PASSWORD = "ContrasenaDemo123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("DELETE FROM users");
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana Pérez", "email": "ana@example.com", "password": "%s"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isCreated());
        userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, "ana@example.com");
    }

    // ---------- Helpers ----------

    private String loginToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@example.com", "password": "%s"}
                                """.formatted(PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asString();
    }

    // Token firmado con la llave REAL de la aplicación; el customizer altera los claims a probar
    private String signedToken(JwtEncoder encoder, Consumer<JwtClaimsSet.Builder> customizer) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("todo-api")
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(30)))
                .claim(JwtConfig.ROLE_CLAIM, "USER");
        customizer.accept(claims);
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private void expectUnauthorized(String token) throws Exception {
        mockMvc.perform(get(TASKS_URL).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    // ---------- PT-06 · Sin token ----------

    @Test
    @DisplayName("PT-06 · GET /tasks sin Authorization → 401")
    void getTasks_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get(TASKS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    @DisplayName("PT-06 · Todo el CRUD y el calendario rechazan peticiones anónimas")
    void taskEndpoints_shouldReturn401WithoutToken() throws Exception {
        String body = """
                {"title": "Tarea anónima", "priority": "LOW"}
                """;
        mockMvc.perform(get(TASKS_URL + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(TASKS_URL).param("from", "2026-10-01").param("to", "2026-10-07"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(TASKS_URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(TASKS_URL + "/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(TASKS_URL + "/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Esquema distinto de Bearer → 401")
    void getTasks_shouldReturn401WithBasicAuthorization() throws Exception {
        mockMvc.perform(get(TASKS_URL).header("Authorization", "Basic YW5hOnNlY3JldA=="))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Token válido ----------

    @Test
    @DisplayName("GET /tasks con token válido → 200")
    void getTasks_shouldReturn200WithValidToken() throws Exception {
        mockMvc.perform(get(TASKS_URL).header("Authorization", "Bearer " + loginToken()))
                .andExpect(status().isOk());
    }

    // ---------- PT-08 · Token modificado ----------

    @Test
    @DisplayName("PT-08 · Token con un carácter de la firma modificado → 401")
    void getTasks_shouldReturn401WhenSignatureIsModified() throws Exception {
        String token = loginToken();
        int i = token.lastIndexOf('.') + 5; // un carácter dentro de la firma
        char replacement = token.charAt(i) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, i) + replacement + token.substring(i + 1);

        expectUnauthorized(tampered);
    }

    @Test
    @DisplayName("PT-08 · Payload alterado (role USER → ADMIN) conservando la firma → 401")
    void getTasks_shouldReturn401WhenPayloadIsModified() throws Exception {
        String[] parts = loginToken().split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        // Decodificar y re-codificar el payload es trivial: por eso solo la firma da garantías
        String payload = new String(decoder.decode(parts[1]), StandardCharsets.UTF_8).replace("\"USER\"", "\"ADMIN\"");
        String tampered = parts[0] + "." + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];

        expectUnauthorized(tampered);
    }

    @Test
    @DisplayName("PT-08 · Token firmado con otra llave → 401")
    void getTasks_shouldReturn401WhenTokenIsSignedWithAnotherKey() throws Exception {
        byte[] otherKey = new byte[32];
        new SecureRandom().nextBytes(otherKey);
        JwtEncoder otherEncoder = new JwtConfig(Base64.getEncoder().encodeToString(otherKey), "todo-api").jwtEncoder();

        expectUnauthorized(signedToken(otherEncoder, claims -> { }));
    }

    // ---------- PT-09 · Token expirado ----------

    @Test
    @DisplayName("PT-09 · Token expirado (firma válida) → 401")
    void getTasks_shouldReturn401WhenTokenIsExpired() throws Exception {
        Instant issuedAt = Instant.now().minus(Duration.ofMinutes(40));
        String expired = signedToken(jwtEncoder, claims -> claims
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(Duration.ofMinutes(30)))); // venció hace 10 minutos

        expectUnauthorized(expired);
    }

    // ---------- Claims incoherentes (firma válida) ----------

    @Test
    @DisplayName("Token de otro emisor (iss) → 401")
    void getTasks_shouldReturn401WhenIssuerIsDifferent() throws Exception {
        expectUnauthorized(signedToken(jwtEncoder, claims -> claims.issuer("otra-api")));
    }

    @Test
    @DisplayName("Token con rol inexistente → 401")
    void getTasks_shouldReturn401WhenRoleIsUnknown() throws Exception {
        expectUnauthorized(signedToken(jwtEncoder, claims -> claims.claim(JwtConfig.ROLE_CLAIM, "SUPERADMIN")));
    }

    @Test
    @DisplayName("Token sin rol → 401")
    void getTasks_shouldReturn401WhenRoleIsMissing() throws Exception {
        expectUnauthorized(signedToken(jwtEncoder, claims -> claims.claims(c -> c.remove(JwtConfig.ROLE_CLAIM))));
    }

    @Test
    @DisplayName("Token con sub no numérico → 401")
    void getTasks_shouldReturn401WhenSubjectIsNotAnId() throws Exception {
        expectUnauthorized(signedToken(jwtEncoder, claims -> claims.subject("ana@example.com")));
    }

    @Test
    @DisplayName("Token válido de un usuario que ya no existe → /users/me responde 404")
    void me_shouldReturn404WhenUserNoLongerExists() throws Exception {
        String token = signedToken(jwtEncoder, claims -> claims.subject("999999"));

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------- CORS ----------

    @Test
    @DisplayName("CORS · El preflight con Authorization desde React pasa sin token")
    void tasks_shouldAllowCorsPreflightWithAuthorizationHeader() throws Exception {
        mockMvc.perform(options(TASKS_URL)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "authorization, content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
