package edu.eci.dosw.todo.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "title", length = 120, nullable = false)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10, nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "dueDate")
    private LocalDate dueDate;

    @Column(name = "createdAt", nullable =  false, updatable = false)
    private LocalDateTime createdAt;

    public TaskEntity() { }

    public long getId() { return this.id; }
    public String getTitle()  { return this.title; }
    public String getDescription()  { return this.description; }
    public TaskStatus getStatus() { return this.status; }
    public TaskPriority getPriority() { return this.priority; }
    public LocalDate getDueDate() { return this.dueDate; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }

    public void setId(long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Bloque para rellenar la fecha automáticamente en Java si creamos una tarea desde el backend
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}