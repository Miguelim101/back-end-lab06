package edu.eci.dosw.todo.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * PostgreSQL 17 real y aislado en Docker para las pruebas (patrón singleton: un solo
 * contenedor para todas las clases). Ejecuta los scripts de database/ en orden (001, 002, 003...)
 * igual que se hace manualmente con todo-postgres, así que las migraciones también quedan probadas.
 */
public final class PostgresTestContainer {

    public static final String IMAGE = "postgres:17-alpine";

    private static final PostgreSQLContainer POSTGRES = withScripts(newContainer(), migrationScripts());

    static {
        POSTGRES.start();
    }

    private PostgresTestContainer() { }

    public static PostgreSQLContainer newContainer() {
        return new PostgreSQLContainer(IMAGE)
                .withDatabaseName("todo_db")
                .withUsername("todo_user")
                .withPassword("todo_password");
    }

    // Scripts .sql de database/ ordenados por nombre
    public static List<Path> migrationScripts() {
        try (Stream<Path> scripts = Files.list(Path.of("database"))) {
            return scripts.filter(script -> script.toString().endsWith(".sql")).sorted().toList();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudieron leer los scripts de database/", ex);
        }
    }

    // /docker-entrypoint-initdb.d ejecuta los .sql en orden alfabético al crear la BD
    private static PostgreSQLContainer withScripts(PostgreSQLContainer container, List<Path> scripts) {
        scripts.forEach(script -> container.withCopyFileToContainer(
                MountableFile.forHostPath(script),
                "/docker-entrypoint-initdb.d/" + script.getFileName()));
        return container;
    }

    public static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
