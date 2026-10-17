package edu.eci.dosw.todo.repository;

import edu.eci.dosw.todo.entity.CategorieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ==========================================================================
 * == PASO 3: Es una interfaz que hereda de JpaRepository que permite      ==
 * ==         obtener de manera fácil las operaciones de una base de datos ==
 * ==========================================================================
 */
@Repository
public interface CategorieRepository extends JpaRepository<CategorieEntity, Long> {

}
