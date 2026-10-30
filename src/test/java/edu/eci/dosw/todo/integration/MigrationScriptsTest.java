package edu.eci.dosw.todo.integration;

import edu.eci.dosw.todo.support.PostgresTestContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de la evolución del esquema (LAB 7 · Parte 2.2): se simula la base de datos del Lab 6
 * con tareas existentes y se aplican 002 y 003 encima. Ninguna tarea se borra y todas quedan
 * asignadas al usuario de prueba antes de exigir NOT NULL.
 */
class MigrationScriptsTest {

    private static final PostgreSQLContainer POSTGRES = PostgresTestContainer.newContainer();
    private static Connection connection;

    @BeforeAll
    static void migrateDatabaseWithPreviousTasks() throws Exception {
        POSTGRES.start();
        connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        runScript("001_create_schema.sql");
        // Datos creados en el Lab 6, antes de que existieran los usuarios
        execute("INSERT INTO tasks (title, status, priority) VALUES "
                + "('Tarea previa 1', 'PENDING', 'HIGH'), ('Tarea previa 2', 'COMPLETED', 'LOW')");

        runScript("002_create_users.sql");
        runScript("003_add_task_owner.sql");
    }

    @AfterAll
    static void stop() throws SQLException {
        connection.close();
        POSTGRES.stop();
    }

    private static void runScript(String name) throws IOException, SQLException {
        execute(Files.readString(Path.of("database", name)));
    }

    private static void execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static long queryLong(String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    @Test
    @DisplayName("Las tareas previas no se borran")
    void migration_shouldNotDeletePreviousTasks() throws SQLException {
        assertThat(queryLong("SELECT COUNT(*) FROM tasks")).isEqualTo(2);
    }

    @Test
    @DisplayName("Las tareas previas quedan asignadas al usuario de prueba legacy")
    void migration_shouldAssignPreviousTasksToLegacyUser() throws SQLException {
        long legacyTasks = queryLong("SELECT COUNT(*) FROM tasks t JOIN users u ON u.id = t.user_id "
                + "WHERE u.email = 'legacy@todo.local'");

        assertThat(legacyTasks).isEqualTo(2);
        assertThat(queryLong("SELECT COUNT(*) FROM tasks WHERE user_id IS NULL")).isZero();
    }

    @Test
    @DisplayName("El usuario legacy no tiene un hash BCrypt válido (no puede iniciar sesión)")
    void migration_shouldNotCreateUsableCredentialsForLegacyUser() throws SQLException {
        long bcryptHashes = queryLong("SELECT COUNT(*) FROM users "
                + "WHERE email = 'legacy@todo.local' AND password_hash LIKE '$2%'");

        assertThat(bcryptHashes).isZero();
    }

    @Test
    @DisplayName("Después de migrar, una tarea sin propietario es rechazada (NOT NULL)")
    void migration_shouldRequireOwner() {
        assertThatThrownBy(() -> execute("INSERT INTO tasks (title) VALUES ('Sin dueño')"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("user_id");
    }

    @Test
    @DisplayName("La llave foránea impide asignar un propietario inexistente")
    void migration_shouldRequireExistingOwner() {
        assertThatThrownBy(() -> execute("INSERT INTO tasks (title, user_id) VALUES ('Huérfana', 999999)"))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("fk_tasks_user");
    }
}
