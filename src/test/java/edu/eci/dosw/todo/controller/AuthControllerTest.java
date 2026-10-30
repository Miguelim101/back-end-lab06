package edu.eci.dosw.todo.controller;

import edu.eci.dosw.todo.config.JsonAccessDeniedHandler;
import edu.eci.dosw.todo.config.JsonAuthenticationEntryPoint;
import edu.eci.dosw.todo.config.JwtConfig;
import edu.eci.dosw.todo.config.SecurityConfig;
import edu.eci.dosw.todo.dto.LoginRequest;
import edu.eci.dosw.todo.dto.RegisterRequest;
import edu.eci.dosw.todo.dto.TokenResponse;
import edu.eci.dosw.todo.dto.UserResponse;
import edu.eci.dosw.todo.entity.Role;
import edu.eci.dosw.todo.entity.UserEntity;
import edu.eci.dosw.todo.exception.EmailAlreadyExistsException;
import edu.eci.dosw.todo.exception.InvalidCredentialsException;
import edu.eci.dosw.todo.service.AuthService;
import edu.eci.dosw.todo.support.AbstractWebMvcSecurityTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class AuthControllerTest extends AbstractWebMvcSecurityTest {

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // ---------- Helper ----------

    private UserResponse buildUser() {
        UserEntity entity = new UserEntity();
        entity.setId(1L);
        entity.setName("Ana Pérez");
        entity.setEmail("ana@example.com");
        entity.setPasswordHash("$2a$10$hash");
        entity.setRole(Role.USER);
        return new UserResponse(entity);
    }

    private String json(RegisterRequest request) {
        return objectMapper.writeValueAsString(request);
    }

    // ---------- POST /auth/register ----------

    @Test
    @DisplayName("POST /auth/register válido → 201 sin contraseña ni hash")
    void register_shouldReturn201WhenRequestIsValid() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(buildUser());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("Ana Pérez", "ana@example.com", "ContrasenaDemo123!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /auth/register es público (sin token) → no 401")
    void register_shouldNotRequireAuthentication() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(buildUser());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("Ana Pérez", "ana@example.com", "ContrasenaDemo123!"))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /auth/register correo inválido → 400")
    void register_shouldReturn400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("Ana Pérez", "no-es-un-correo", "ContrasenaDemo123!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El correo no tiene un formato válido"));

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /auth/register contraseña < 8 caracteres → 400")
    void register_shouldReturn400WhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("Ana Pérez", "ana@example.com", "corta"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La contraseña debe tener entre 8 y 72 caracteres"));

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /auth/register nombre vacío → 400")
    void register_shouldReturn400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("   ", "ana@example.com", "ContrasenaDemo123!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El nombre no puede ser vacío"));

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /auth/register correo duplicado → 409")
    void register_shouldReturn409WhenEmailAlreadyExists() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("ana@example.com"));

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest("Ana Pérez", "ana@example.com", "ContrasenaDemo123!"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ---------- POST /auth/login ----------

    @Test
    @DisplayName("POST /auth/login válido → 200 con token Bearer")
    void login_shouldReturn200WithTokenWhenCredentialsAreValid() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new TokenResponse("header.payload.signature", 1800));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ana@example.com", "ContrasenaDemo123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("header.payload.signature"))
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    @DisplayName("POST /auth/login credenciales inválidas → 401 sin token")
    void login_shouldReturn401WithoutTokenWhenCredentialsAreInvalid() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ana@example.com", "incorrecta"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("POST /auth/login sin contraseña → 400")
    void login_shouldReturn400WhenPasswordIsMissing() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ana@example.com", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La contraseña no puede ser vacía"));

        verify(authService, never()).login(any());
    }
}
