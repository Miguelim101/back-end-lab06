package edu.eci.dosw.todo.repository;

import edu.eci.dosw.todo.entity.Role;
import edu.eci.dosw.todo.entity.UserEntity;
import edu.eci.dosw.todo.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Restricciones de la tabla users en PostgreSQL real (LAB 7 · Parte 2.5): correo único y en minúsculas.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.registerDatasource(registry);
    }

    @Autowired
    private UserRepository userRepository;

    private UserEntity newUser(String email) {
        UserEntity user = new UserEntity();
        user.setName("Ana Pérez");
        user.setEmail(email);
        user.setPasswordHash("$2a$10$hash");
        user.setRole(Role.USER);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void findByEmail_shouldReturnSavedUser() {
        userRepository.saveAndFlush(newUser("ana.unique@example.com"));

        assertThat(userRepository.findByEmail("ana.unique@example.com")).isPresent();
        assertThat(userRepository.existsByEmail("ana.unique@example.com")).isTrue();
    }

    @Test
    void save_shouldNotAllowDuplicateEmail() {
        userRepository.saveAndFlush(newUser("ana.dup@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("ana.dup@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_shouldNotAllowUppercaseEmail() {
        // CHECK (email = LOWER(email)): la BD también impide duplicados equivalentes
        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("Ana.Mayus@Example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
