package edu.eci.dosw.todo.repository;

import edu.eci.dosw.todo.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
