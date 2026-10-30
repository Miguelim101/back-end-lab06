package edu.eci.dosw.todo.repository;

import edu.eci.dosw.todo.entity.TaskEntity;
import edu.eci.dosw.todo.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ==========================================================================
 * == PASO 3: Es una interfaz que hereda de JpaRepository que permite      ==
 * ==         obtener de manera fácil las operaciones de una base de datos ==
 * ==========================================================================
 */
@Repository
// En este caso, <TaskEntity, Long> tiene dos parámetros donde TaskEntity es la clase que tiene que mapear
// y Long es el tipo de dato que correponde al id de la clase TaskEntity (mapeo por id)
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    // Aquí tenemos todas las operaciones de la base de datos heredadas (incluido todo el GRUD)
    // taskRepository.save(unaTarea); Inserta o actualiza una tarea.
    // taskRepository.findAll(); Devuelve una lista con todas las tareas.
    // taskRepository.findById(1L); Busca una tarea por su ID.
    // taskRepository.deleteById(1L); Elimina una tarea por su ID.

    // LAB 7 (HU-SEG-03): TODAS las consultas de la API se filtran por propietario (owner.id).
    // Una tarea ajena se comporta igual que una inexistente: nunca se devuelve ni se modifica.

    // SELECT * FROM tasks WHERE user_id = :ownerId
    List<TaskEntity> findAllByOwnerId(Long ownerId);

    // SELECT * FROM tasks WHERE id = :id AND user_id = :ownerId
    Optional<TaskEntity> findByIdAndOwnerId(Long id, Long ownerId);

    // SELECT COUNT(*) > 0 FROM tasks WHERE id = :id AND user_id = :ownerId
    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    // BONUS (calendario semanal) solo con las tareas propias
    // -> SELECT * FROM tasks WHERE user_id = :ownerId AND due_date BETWEEN :from AND :to (ambos extremos incluidos)
    List<TaskEntity> findByOwnerIdAndDueDateBetween(Long ownerId, LocalDate from, LocalDate to);

    // LAB 7 (HU-SEG-05): métrica agregada para el resumen administrativo (no expone tareas)
    // -> SELECT COUNT(*) FROM tasks WHERE status = :status
    long countByStatus(TaskStatus status);
}
