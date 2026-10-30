package edu.eci.dosw.todo.repository;

import edu.eci.dosw.todo.entity.Role;
import edu.eci.dosw.todo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ==========================================================================
 * == LAB 7: Acceso a la tabla users. El correo llega siempre normalizado  ==
 * ==        (minúsculas y sin espacios) desde la capa de servicio         ==
 * ==========================================================================
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // SELECT * FROM users WHERE email = :email
    Optional<UserEntity> findByEmail(String email);

    // SELECT COUNT(*) > 0 FROM users WHERE email = :email
    boolean existsByEmail(String email);

    // LAB 7 (HU-SEG-05): SELECT COUNT(*) FROM users WHERE role = :role
    long countByRole(Role role);
}
