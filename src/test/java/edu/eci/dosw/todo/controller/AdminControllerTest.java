package edu.eci.dosw.todo.controller;

import edu.eci.dosw.todo.config.JsonAccessDeniedHandler;
import edu.eci.dosw.todo.config.JsonAuthenticationEntryPoint;
import edu.eci.dosw.todo.config.JwtConfig;
import edu.eci.dosw.todo.config.SecurityConfig;
import edu.eci.dosw.todo.dto.AdminSummaryResponse;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.service.AdminService;
import edu.eci.dosw.todo.support.AbstractWebMvcSecurityTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class AdminControllerTest extends AbstractWebMvcSecurityTest {

    private static final String SUMMARY_URL = "/api/v1/admin/summary";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    // JWT de prueba con la autoridad que produce JwtConfig a partir del claim role
    private static JwtRequestPostProcessor withRole(String role) {
        return jwt().jwt(token -> token.subject("1").claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    @DisplayName("GET /admin/summary con rol ADMIN → 200 con métricas agregadas")
    void summary_shouldReturn200ForAdmin() throws Exception {
        Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
        byStatus.put(TaskStatus.PENDING, 2L);
        byStatus.put(TaskStatus.IN_PROGRESS, 1L);
        byStatus.put(TaskStatus.COMPLETED, 0L);
        when(adminService.summary()).thenReturn(new AdminSummaryResponse(2, 1, 3, byStatus));

        mockMvc.perform(get(SUMMARY_URL).with(withRole("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.totalAdmins").value(1))
                .andExpect(jsonPath("$.totalTasks").value(3))
                .andExpect(jsonPath("$.tasksByStatus.PENDING").value(2));
    }

    @Test
    @DisplayName("GET /admin/summary con rol USER → 403 y el servicio no se ejecuta")
    void summary_shouldReturn403ForUser() throws Exception {
        mockMvc.perform(get(SUMMARY_URL).with(withRole("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(adminService);
    }

    @Test
    @DisplayName("GET /admin/summary sin token → 401")
    void summary_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(adminService);
    }
}
