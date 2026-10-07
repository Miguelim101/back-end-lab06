package edu.eci.dosw.todo.dto;

import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Objects;

/**
 * title
 * description
 * status
 * priority
 * dueDate
 */
public class TaskUpdateRequest {

    @NotBlank(message = "El título no puede ser vacío")
    @Size(max = 120, message = "El título no puede superar los 120 caracteres")
    private String title;

    @Size(max = 500, message = "La descripción debe tener no más de 500 caracteres")
    private String description;

    @NotNull(message = "El estado no puede ser vacío")
    private TaskStatus status = TaskStatus.PENDING;

    @NotNull(message = "La prioridad no puede ser vacía")
    private TaskPriority priority = TaskPriority.MEDIUM;

    @FutureOrPresent(message = "La fecha de vencimiento debe ser una fecha futura")
    private LocalDate dueDate;

    // Constructors
    public TaskUpdateRequest () { }
    public TaskUpdateRequest (String title,
                              String description,
                              TaskStatus status,
                              TaskPriority priority,
                              LocalDate dueDate){
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    public String getTitle()  { return this.title; }
    public String getDescription()  { return this.description; }
    public TaskStatus getStatus() { return this.status; }
    public TaskPriority getPriority() { return this.priority; }
    public LocalDate getDueDate() { return this.dueDate; }

    // public void setTitle(String title) { this.title = title; }
    // public void setDescription(String description) { this.description = description; }
    // public void setStatus(TaskStatus status) { this.status = status; }
    // public void setPriority(TaskPriority priority) { this.priority = priority; }
    // public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskUpdateRequest that = (TaskUpdateRequest) o;

        return Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                Objects.equals(status, that.status) &&
                Objects.equals(priority, that.priority) &&
                Objects.equals(dueDate, that.dueDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, status, priority, dueDate);
    }
}
