package edu.eci.dosw.todo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.eci.dosw.todo.entity.TaskEntity;
import edu.eci.dosw.todo.entity.TaskPriority;
import edu.eci.dosw.todo.entity.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * id
 * title
 * description
 * status
 * priority
 * dueDate
 * createdAt
 */
//@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL) // Oculta propiedades que vengan en null
public class TaskResponse {

    private long id;
    private String title;
    private String description;
    private TaskStatus status = TaskStatus.PENDING;
    private TaskPriority priority = TaskPriority.MEDIUM;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate dueDate;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime createdAt;

    // Constructors
    public TaskResponse (TaskEntity entity){
        this.id = entity.getId();
        this.title = entity.getTitle();
        this.description = entity.getDescription();
        this.status = entity.getStatus();
        this.priority = entity.getPriority();
        this.dueDate = entity.getDueDate();
        this.createdAt = entity.getCreatedAt();
    }

    public long getId() { return this.id; }
    public String getTitle()  { return this.title; }
    public String getDescription()  { return this.description; }
    public TaskStatus getStatus() { return this.status; }
    public TaskPriority getPriority() { return this.priority; }
    public LocalDate getDueDate() { return this.dueDate; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskResponse that = (TaskResponse) o;

        return Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                Objects.equals(status, that.status) &&
                Objects.equals(priority, that.priority) &&
                Objects.equals(dueDate, that.dueDate) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, status, priority, dueDate, createdAt);
    }
}
