package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;

import java.util.List;

/**
 * =================================================================================
 * == PASO 5.1: Esta interfaz establece el contrato que debe seguir la lógica     ==
 * ==         de negocio (este es el modelo para diseñar el cerebro, que sería la ==
 * ==         clase que implementa esta interfaz. Establece el CRUD básico)        ==
 * =================================================================================
 */
public interface TaskService {
    List<TaskResponse> findAll();
    TaskResponse findById(Long id);
    TaskResponse create(TaskCreateRequest request);
    TaskResponse update(Long id, TaskUpdateRequest request);
    void delete(Long id);
}
