package edu.eci.dosw.todo.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base de las pruebas @WebMvcTest: aporta la llave JWT de prueba que necesita JwtConfig.
 * Cada subclase importa SecurityConfig, JwtConfig y JsonAuthenticationEntryPoint para
 * ejecutar las reglas reales de Spring Security en el slice web.
 */
public abstract class AbstractWebMvcSecurityTest {

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        JwtTestSupport.registerJwtProperties(registry);
    }
}
