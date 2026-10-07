package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.entity.TaskEntity;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ======================================================================================
 * == PASO 5.2: Es el cerebro que gestiona la lógica de negocio, donde se implementa   ==
 * ==         el contrato esblecido por la interfaz del servicio. Aquí se toma la       ==
 * ==         decisión de qué hacer. El Servicio recibe un DTO limpio del controlador, ==
 * ==         aplica la lógica                                                         ==
 * ======================================================================================
 */
@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> findAll() {
        return taskRepository.findAll().stream()
                .map(TaskResponse::new)
                .collect(Collectors.toList()); // La respuesta contiene una lista con los datos de la Task filtrando con
                                               // TaskResponse (evitando enviar datos que no se deben)
    }

    @Override
    @Transactional
    public TaskResponse findById(Long id) {
        TaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TAREA no encontrada con ID " + id));
        return new TaskResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse create(TaskCreateRequest request) {
        TaskEntity task = new TaskEntity();

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());

        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        else {
            task.setPriority(TaskPriority.MEDIUM);
        }

        task.setStatus(TaskStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());

        TaskEntity savedTask = taskRepository.save(task);
        return new TaskResponse(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse update(Long id, TaskUpdateRequest request) {
        TaskEntity existingtTask = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("NO existe una tarea con el ID " + id));

        existingtTask.setTitle(request.getTitle());
        existingtTask.setDescription(request.getDescription());
        existingtTask.setStatus(request.getStatus());
        existingtTask.setPriority(request.getPriority());
        existingtTask.setDueDate(request.getDueDate());

        TaskEntity updatedTask = taskRepository.save(existingtTask);
        return new TaskResponse(updatedTask);
    }

    @Override
    public void delete(Long id) {
        if(!taskRepository.existsById(id)){
            throw new RuntimeException("NO hay una tarea con ID: " + id);
        }
        taskRepository.deleteById(id);
    }
}
