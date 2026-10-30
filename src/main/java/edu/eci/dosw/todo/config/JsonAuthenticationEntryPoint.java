package edu.eci.dosw.todo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * ==========================================================================
 * == LAB 7: Respuesta 401 cuando falta el token o es inválido / expirado. ==
 * ==        Mantiene el formato {status, message} de GlobalExceptionHandler ==
 * ==        y la cabecera estándar WWW-Authenticate: Bearer                 ==
 * ==========================================================================
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        bearerEntryPoint.commence(request, response, authException); // status 401 + WWW-Authenticate

        // Mensaje genérico: no se detalla por qué falló (ni se registra el token)
        String message = request.getHeader("Authorization") == null
                ? "Authentication is required"
                : "Invalid or expired token";

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":401,\"message\":\"" + message + "\"}");
    }
}
