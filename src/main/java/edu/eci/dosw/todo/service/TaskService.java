package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * =================================================================================
 * == PASO 5.1: Esta interfaz establece el contrato que debe seguir la lógica     ==
 * ==         de negocio (este es el modelo para diseñar el cerebro, que sería la ==
 * ==         clase que implementa esta interfaz. Establece el CRUD básico)        ==
 * =================================================================================
 */
public interface TaskService {
    // LAB 7 (HU-SEG-03): userId es el usuario autenticado (claim sub del JWT); cada operación
    // actúa SOLO sobre sus tareas. Una tarea ajena se trata como inexistente (TaskNotFoundException -> 404)
    List<TaskResponse> findAll(Long userId);
    TaskResponse findById(Long userId, Long id);
    TaskResponse create(Long userId, TaskCreateRequest request);
    TaskResponse update(Long userId, Long id, TaskUpdateRequest request);
    void delete(Long userId, Long id);

    // BONUS (calendario semanal): tareas propias cuya fecha límite está entre from y to (incluidos)
    List<TaskResponse> findByDueDateBetween(Long userId, LocalDate from, LocalDate to);
}
