package edu.eci.dosw.todo.dto;

import edu.eci.dosw.todo.entity.TaskPriority;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * title
 * description
 * priority
 * dueDate
 */
public class TaskCreateRequest {

    @NotBlank(message = "El título no puede ser vacío")
    @Size(max = 120, message = "El título no puede superar los 120 caracteres")
    private String title;

    @Size(max = 500, message = "La descripción debe tener no más de 500 caracteres")
    private String description;

    @NotBlank(message = "La prioridad no puede ser vacía")
    @Size(max = 10, message = "La prioridad no debe tener maś de 10 caracteres")
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Future(message = "La fecha de vencimiento debe ser una fecha futura")
    private LocalDate dueDate;

    // Constructors
    public TaskCreateRequest () { }
    public TaskCreateRequest (String title,
                              String description,
                              TaskPriority priority,
                              LocalDate dueDate){
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    public String getTitle()  { return this.title; }
    public String getDescription()  { return this.description; }
    public TaskPriority getPriority() { return this.priority; }
    public LocalDate getDueDate() { return this.dueDate; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
