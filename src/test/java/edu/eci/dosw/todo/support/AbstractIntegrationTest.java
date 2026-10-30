package edu.eci.dosw.todo.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base de las pruebas de integración: levanta la aplicación completa (cadena de
 * Spring Security incluida) contra el PostgreSQL de PostgresTestContainer.
 * Spring reutiliza el mismo contexto entre clases.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        PostgresTestContainer.registerDatasource(registry);
        JwtTestSupport.registerJwtProperties(registry);
    }
}
