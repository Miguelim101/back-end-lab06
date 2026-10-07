package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.entity.TaskEntity;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.exception.TaskNotFoundException;
import edu.eci.dosw.todo.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * JUnit 5: Es el motor que corre los tests (etiquetas como @Test, @BeforeEach)
 * Mockito: Sirve para simular (falsificar) el comportamiento del Repositorio o la base de datos
 *          de Docker. Así se prueba la lógica sin tocar datos reales.
 * AssertJ: Da una forma muy humana y legible de escribir las verificaciones con su famoso método
 *          assertThat()
 */
@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository; // 1. El repositorio es un Mock

    @InjectMocks
    private TaskServiceImpl taskServiceImpl;  // 2. Inyectar el mock en el servicio real

    private TaskResponse sampleTaskResponse;
    private TaskEntity sampleTask;

    @BeforeEach
    void SetUp(){
        sampleTask = new TaskEntity();
        sampleTask.setId(1L);
        sampleTask.setTitle("Tests del servicio");
        sampleTask.setDescription("Completar la parte 6 del laboratorio 6");
        sampleTask.setStatus(TaskStatus.PENDING);
        sampleTask.setPriority(TaskPriority.MEDIUM);
        sampleTaskResponse = new TaskResponse(sampleTask);
    }

    // --- PRUEBAS DE LISTAR (findAll) ---

    @Test
    void findAll_shouldReturnTasks(){
        // GIVEN (preparation and repository and docker wile)
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        // WHEN (execution)
        List<TaskResponse> result = taskServiceImpl.findAll();

        // THEN (results)
        assertThat(result).hasSize(1).contains(sampleTaskResponse);
        verify(taskRepository, times(1)).findAll();
    }

    // --- PRUEBAS DE BUSCAR POR ID (findById) ---

    @Test
    void findById_shouldReturnTaskWhenExists(){
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

        TaskResponse result = taskServiceImpl.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    void findById_shouldThrowExceptionWhenTaskDoesNotExist(){
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskServiceImpl.findById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task with id 99 was not found");
    }

    // --- PRUEBAS DE CREAR (create) ---

    @Test
    void create_shouldCreateTask(){
        // 1. Arrange (Preparar datos)
        TaskCreateRequest request = new TaskCreateRequest(
                "Probar el método create()",  // title
                "Dedemos crear el DTO (request), luego simular la task, finalmente el resto", // description
                null, // priority nulo para forzar el valor por defecto
                LocalDate.now().plusDays(1)); // dueDate (mejor usar fecha futura para cumplir @Future)

        // Creamos la Entidad que simulará ser guardada y retornada por el repositorio
        TaskEntity taskSaved = new TaskEntity();
        taskSaved.setId(1L); // CLAVE: Simular que la base de datos asignó un ID
        taskSaved.setTitle(request.getTitle());
        taskSaved.setDescription(request.getDescription());
        taskSaved.setDueDate(request.getDueDate());
        taskSaved.setPriority(TaskPriority.MEDIUM); // La lógica del servicio asigna MEDIUM si request es null
        taskSaved.setStatus(TaskStatus.PENDING); // La lógica del servicio asigna PENDING por defecto
        taskSaved.setCreatedAt(LocalDateTime.now()); // Simula la asignación de fecha de creación

        // 2. Mocking (Configurar el comportamiento ANTES de ejecutar la acción)
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(taskSaved);

        // 3. Act (Ejecutar la acción)
        TaskResponse result = taskServiceImpl.create(request);

        // 4. Assert (Verificaciones)
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L); // valor simulado
        assertThat(result.getTitle()).isEqualTo("Probar el método create()");
        assertThat(result.getDescription()).isEqualTo("Dedemos crear el DTO (request), luego simular la task, finalmente el resto");
        assertThat(result.getPriority()).isEqualTo(TaskPriority.MEDIUM); // Verifica lógica de fallback
        assertThat(result.getDueDate()).isEqualTo(request.getDueDate());

        // Verificación de Mockito
        verify(taskRepository, times(1)).save(any(TaskEntity.class));
    }

    @Test
    void create_shouldAssignDefaultStatus(){
        // Arrange
        TaskCreateRequest request = new TaskCreateRequest(
                "Prueba estado", "Descripción", TaskPriority.HIGH, LocalDate.now().plusDays(2));

        TaskEntity taskSaved = new TaskEntity();
        taskSaved.setId(2L);
        taskSaved.setTitle(request.getTitle());
        taskSaved.setStatus(TaskStatus.PENDING); // Simula la asignación por defecto del servicio

        when(taskRepository.save(any(TaskEntity.class))).thenReturn(taskSaved);

        // Act
        TaskResponse result = taskServiceImpl.create(request);

        // Assert
        assertThat(result.getStatus()).isEqualTo(TaskStatus.PENDING);
        verify(taskRepository, times(1)).save(any(TaskEntity.class));
    }

    // --- PRUEBAS DE ACTUALIZAR (update) ---

    @Test
    void update_shouldUpdateExistingTask(){
        // Arrange: DTO de entrada
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Título actualizado",
                "Descripción actualizada",
                TaskStatus.PENDING,
                TaskPriority.HIGH,
                LocalDate.now().plusDays(5));

        // Arrange: Entidad existente retornada por el findById
        TaskEntity existingTask = new TaskEntity();
        existingTask.setId(1L);
        existingTask.setTitle("Título original");

        // Arrange: Entidad modificada retornada por el save
        TaskEntity updatedTask = new TaskEntity();
        updatedTask.setId(1L);
        updatedTask.setTitle(request.getTitle());
        updatedTask.setDescription(request.getDescription());
        updatedTask.setStatus(request.getStatus());
        updatedTask.setPriority(request.getPriority());
        updatedTask.setDueDate(request.getDueDate());

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(TaskEntity.class))).thenReturn(updatedTask);

        // Act
        TaskResponse result = taskServiceImpl.update(1L, request);

        // Assert
        assertThat(result.getTitle()).isEqualTo("Título actualizado");
        assertThat(result.getDescription()).isEqualTo("Descripción actualizada");
        assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);

        // Verifica el flujo: primero busca, luego guarda
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(TaskEntity.class));
    }

    @Test
    void update_shouldThrowExceptionWhenTaskDoesNotExist(){
        // Arrange
        TaskUpdateRequest request = new TaskUpdateRequest();
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> taskServiceImpl.update(99L, request))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task with id 99 was not found");

        // Verifica que la ejecución se abortó y NUNCA se llamó al método save
        verify(taskRepository, times(1)).findById(99L);
        verify(taskRepository, never()).save(any(TaskEntity.class));
    }

    // --- PRUEBAS DE BORRAR (delete) ---

    @Test
    void delete_shouldDeleteExistingTask(){
        // Arrange
        when(taskRepository.existsById(1L)).thenReturn(true);

        // Act
        taskServiceImpl.delete(1L);

        // Assert
        verify(taskRepository, times(1)).existsById(1L);
        verify(taskRepository, times(1)).deleteById(1L);
    }

    @Test
    void delete_shouldThrowExceptionWhenTaskDoesNotExist(){
        // Arrange
        when(taskRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> taskServiceImpl.delete(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task with id 99 was not found");

        // Verifica que se consultó la existencia, pero NUNCA se intentó borrar
        verify(taskRepository, times(1)).existsById(99L);
        verify(taskRepository, never()).deleteById(anyLong());
    }

}
