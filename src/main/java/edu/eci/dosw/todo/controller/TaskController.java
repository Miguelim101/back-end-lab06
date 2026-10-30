package edu.eci.dosw.todo.controller;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService){
        this.taskService = taskService;
    }

    // LAB 7 (HU-SEG-03): el usuario sale del JWT ya validado (claim sub), nunca del body ni de la URL
    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to){
        // Sin parámetros se conserva el listado completo; con rango (BONUS calendario) se filtra por dueDate
        if (from == null && to == null) {
            return ResponseEntity.ok(taskService.findAll(currentUserId(jwt)));
        }
        return ResponseEntity.ok(taskService.findByDueDateBetween(currentUserId(jwt), from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id){
        return ResponseEntity.ok(taskService.findById(currentUserId(jwt), id));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody TaskCreateRequest request){
        TaskResponse createdTask = taskService.create(currentUserId(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                   @Valid @RequestBody TaskUpdateRequest request){
        return ResponseEntity.ok(taskService.update(currentUserId(jwt), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id){
        taskService.delete(currentUserId(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
