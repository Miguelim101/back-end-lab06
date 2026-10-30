package edu.eci.dosw.todo.repository;

import edu.eci.dosw.todo.entity.Role;
import edu.eci.dosw.todo.entity.TaskEntity;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.entity.UserEntity;
import edu.eci.dosw.todo.support.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de repositorio (LAB 7 · Parte 2.6) contra PostgreSQL real: las consultas por
 * propietario solo devuelven tareas propias y nunca las de otro usuario.
 * Cada prueba corre en una transacción que se revierte al terminar.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // Usa PostgreSQL, no una BD en memoria
class TaskRepositoryTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.registerDatasource(registry);
    }

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity ana;
    private UserEntity luis;
    private TaskEntity anaTask;
    private TaskEntity luisTask;

    private UserEntity saveUser(String name, String email) {
        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$hash");
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.saveAndFlush(user);
    }

    private TaskEntity saveTask(String title, UserEntity owner, LocalDate dueDate) {
        TaskEntity task = new TaskEntity();
        task.setTitle(title);
        task.setStatus(TaskStatus.PENDING);
        task.setPriority(TaskPriority.MEDIUM);
        task.setDueDate(dueDate);
        task.setCreatedAt(LocalDateTime.now());
        task.setOwner(owner);
        return taskRepository.saveAndFlush(task);
    }

    @BeforeEach
    void setUp() {
        ana = saveUser("Ana Pérez", "ana.repo@example.com");
        luis = saveUser("Luis Gómez", "luis.repo@example.com");
        anaTask = saveTask("Tarea de Ana", ana, LocalDate.of(2026, 10, 22));
        luisTask = saveTask("Tarea de Luis", luis, LocalDate.of(2026, 10, 23));
    }

    @Test
    void findAllByOwnerId_shouldReturnOnlyOwnTasks() {
        List<TaskEntity> anaTasks = taskRepository.findAllByOwnerId(ana.getId());

        assertThat(anaTasks).extracting(TaskEntity::getTitle).containsExactly("Tarea de Ana");
    }

    @Test
    void findByIdAndOwnerId_shouldReturnOwnTask() {
        assertThat(taskRepository.findByIdAndOwnerId(anaTask.getId(), ana.getId())).isPresent();
    }

    @Test
    void findByIdAndOwnerId_shouldNotReturnTaskOfAnotherUser() {
        assertThat(taskRepository.findByIdAndOwnerId(luisTask.getId(), ana.getId())).isEmpty();
    }

    @Test
    void existsByIdAndOwnerId_shouldNotFindTaskOfAnotherUser() {
        assertThat(taskRepository.existsByIdAndOwnerId(anaTask.getId(), ana.getId())).isTrue();
        assertThat(taskRepository.existsByIdAndOwnerId(anaTask.getId(), luis.getId())).isFalse();
    }

    @Test
    void findByOwnerIdAndDueDateBetween_shouldNotReturnTasksOfAnotherUser() {
        List<TaskEntity> week = taskRepository.findByOwnerIdAndDueDateBetween(
                ana.getId(), LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 25));

        // La tarea de Luis cae en la misma semana, pero no es de Ana
        assertThat(week).extracting(TaskEntity::getTitle).containsExactly("Tarea de Ana");
    }

    @Test
    void save_shouldNotAllowTaskWithoutOwner() {
        assertThatThrownBy(() -> saveTask("Tarea sin dueño", null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
