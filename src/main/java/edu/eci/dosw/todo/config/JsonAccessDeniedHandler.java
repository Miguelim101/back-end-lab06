package edu.eci.dosw.todo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ==========================================================================
 * == LAB 7: Respuesta 403 cuando el JWT es válido pero el rol no alcanza  ==
 * ==        (por ejemplo, un USER en /admin/summary). Mismo formato       ==
 * ==        {status, message} que el resto de errores de la API          ==
 * ==========================================================================
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":403,\"message\":\"Access denied: insufficient role\"}");
    }
}
