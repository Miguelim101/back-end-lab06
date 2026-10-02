package edu.eci.dosw.todo.repository;

import edu.eci.dosw.todo.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    // Aquí tenemos todas las operaciones de la base de datos heredadas (incluido todo el GRUD)
    // taskRepository.save(unaTarea); Inserta o actualiza una tarea.
    // taskRepository.findAll(); Devuelve una lista con todas las tareas.
    // taskRepository.findById(1L); Busca una tarea por su ID.
    // taskRepository.deleteById(1L); Elimina una tarea por su ID.
}
