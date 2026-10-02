package edu.eci.dosw.todo.entity;

import jakarta.persistence.*;
import edu.eci.dosw.todo.entity.TaskStatus;
import edu.eci.dosw.todo.entity.TaskPriority;

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

    @Column(name = "due_date")
    private LocalDate due_date;

    @Column(name = "created_at", nullable =  false, updatable = false)
    private LocalDateTime created_at;

    public TaskEntity() { }


    // Bloque para rellenar la fecha automáticamente en Java si creamos una tarea desde el backend
    @PrePersist
    protected void onCreate() {
        if (this.created_at == null) {
            this.created_at = LocalDateTime.now();
        }
    }
}