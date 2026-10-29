package edu.eci.dosw.todo.controller;

import edu.eci.dosw.todo.dto.TaskCreateRequest;
import edu.eci.dosw.todo.dto.TaskResponse;
import edu.eci.dosw.todo.dto.TaskUpdateRequest;
import edu.eci.dosw.todo.entity.TaskEntity;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.exception.InvalidDateRangeException;
import edu.eci.dosw.todo.exception.TaskNotFoundException;
import edu.eci.dosw.todo.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final String BASE_URL = "/api/v1/tasks";

    @Autowired
    private MockMvc mockMvc;  // Se encarga de simular las peticiones HTTP

    @Autowired
    private ObjectMapper objectMapper;  // Convierte objetos Java <-> JSON

    @MockitoBean
    private TaskService taskService;  // Mockea el servicio en el contenedor de Spring

    private TaskResponse sampleResponse;

    // ---------- Helper ----------

    private TaskResponse buildResponse(long id, String title, String description) {
        TaskEntity entity = new TaskEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setStatus(TaskStatus.PENDING);
        entity.setPriority(TaskPriority.MEDIUM);
        entity.setDueDate(LocalDate.now().plusDays(7));
        entity.setCreatedAt(LocalDateTime.now());
        return new TaskResponse(entity);
    }

    // ---------- GET /tasks ----------

    @Test
    @DisplayName("GET /tasks → 200")
    void getAllTasks_returns200() throws Exception {
        when(taskService.findAll())
                .thenReturn(List.of(buildResponse(1L, "Estudiar", "Repasar Spring")));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Estudiar"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // ---------- GET /tasks/{id} ----------

    @Test
    @DisplayName("GET /tasks/{id} existente → 200")
    void getTaskById_existing_returns200() throws Exception {
        when(taskService.findById(1L))
                .thenReturn(buildResponse(1L, "Estudiar", "Repasar Spring"));

        mockMvc.perform(get(BASE_URL + "/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Estudiar"));
    }

    @Test
    @DisplayName("GET /tasks/{id} inexistente → 404")
    void getTaskById_notFound_returns404() throws Exception {
        when(taskService.findById(99L)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get(BASE_URL + "/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------- POST /tasks ----------

    @Test
    @DisplayName("POST /tasks válido → 201")
    void createTask_valid_returns201() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest(
                "Estudiar", "BASE_URRepasar Spring", TaskPriority.MEDIUM, LocalDate.now().plusDays(7));
        when(taskService.create(any(TaskCreateRequest.class)))
                .thenReturn(buildResponse(1L, "Estudiar", "Repasar Spring"));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Estudiar"));
    }

    @Test
    @DisplayName("POST /tasks inválido (título vacío) → 400")
    void createTask_invalid_returns400() throws Exception {
        TaskCreateRequest invalid = new TaskCreateRequest(
                "", "Sin título", TaskPriority.MEDIUM, LocalDate.now().plusDays(7));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // ---------- PUT /tasks/{id} ----------

    @Test
    @DisplayName("PUT /tasks/{id} existente → 200")
    void updateTask_existing_returns200() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Estudiar más", "Repasar JPA", TaskStatus.PENDING, TaskPriority.MEDIUM,
                LocalDate.now().plusDays(10));
        when(taskService.update(eq(1L), any(TaskUpdateRequest.class)))
                .thenReturn(buildResponse(1L, "Estudiar más", "Repasar JPA"));

        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Estudiar más"));
    }

    @Test
    @DisplayName("PUT /tasks/{id} inexistente → 404")
    void updateTask_notFound_returns404() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest(
                "Estudiar más", "Repasar JPA", TaskStatus.PENDING, TaskPriority.MEDIUM,
                LocalDate.now().plusDays(10));
        when(taskService.update(eq(99L), any(TaskUpdateRequest.class)))
                .thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(put(BASE_URL + "/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---------- DELETE /tasks/{id} ----------

    @Test
    @DisplayName("DELETE /tasks/{id} existente → 204")
    void deleteTask_existing_returns204() throws Exception {
        doNothing().when(taskService).delete(1L);

        mockMvc.perform(delete(BASE_URL + "/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(taskService).delete(1L);
    }

    // ---------- GET /tasks?from=&to= (BONUS calendario) ----------

    @Test
    @DisplayName("GET /tasks?from&to válido → 200")
    void getTasksByRange_valid_returns200() throws Exception {
        LocalDate from = LocalDate.of(2026, 9, 21);
        LocalDate to = LocalDate.of(2026, 9, 27);
        when(taskService.findByDueDateBetween(from, to))
                .thenReturn(List.of(buildResponse(1L, "Lab DOSW", "Entrega del bono")));

        mockMvc.perform(get(BASE_URL).param("from", "2026-09-21").param("to", "2026-09-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Lab DOSW"));

        verify(taskService).findByDueDateBetween(from, to);
    }

    @Test
    @DisplayName("GET /tasks?from&to rango invertido → 400")
    void getTasksByRange_invertedRange_returns400() throws Exception {
        when(taskService.findByDueDateBetween(any(), any()))
                .thenThrow(new InvalidDateRangeException("'from' date must be before or equal to 'to' date"));

        mockMvc.perform(get(BASE_URL).param("from", "2026-09-27").param("to", "2026-09-21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /tasks?from (sin to) → 400")
    void getTasksByRange_missingParam_returns400() throws Exception {
        when(taskService.findByDueDateBetween(any(), isNull()))
                .thenThrow(new InvalidDateRangeException("Both 'from' and 'to' dates are required"));

        mockMvc.perform(get(BASE_URL).param("from", "2026-09-21"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /tasks?from&to con formato inválido → 400")
    void getTasksByRange_invalidFormat_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL).param("from", "21-09-2026").param("to", "2026-09-27"))
                .andExpect(status().isBadRequest());
    }
}