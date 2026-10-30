package edu.eci.dosw.todo.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Base de las pruebas de integración: levanta la aplicación completa (cadena de
 * Spring Security incluida) contra un PostgreSQL 17 real y aislado en Docker.
 *
 * El contenedor ejecuta los scripts de database/ en orden (001, 002, ...) igual que
 * se hace manualmente con todo-postgres, así que las migraciones también quedan probadas.
 * Se usa un único contenedor para todas las clases (patrón singleton) y Spring reutiliza el contexto.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer POSTGRES = createContainer();

    static {
        POSTGRES.start();
    }

    private static PostgreSQLContainer createContainer() {
        PostgreSQLContainer container = new PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("todo_db")
                .withUsername("todo_user")
                .withPassword("todo_password");

        // /docker-entrypoint-initdb.d ejecuta los .sql en orden alfabético al crear la BD
        try (Stream<Path> scripts = Files.list(Path.of("database"))) {
            scripts.filter(script -> script.toString().endsWith(".sql"))
                    .sorted()
                    .forEach(script -> container.withCopyFileToContainer(
                            MountableFile.forHostPath(script),
                            "/docker-entrypoint-initdb.d/" + script.getFileName()));
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudieron leer los scripts de database/", ex);
        }
        return container;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
