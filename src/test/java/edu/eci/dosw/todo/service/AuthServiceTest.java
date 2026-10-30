package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.RegisterRequest;
import edu.eci.dosw.todo.dto.UserResponse;
import edu.eci.dosw.todo.entity.Role;
import edu.eci.dosw.todo.entity.UserEntity;
import edu.eci.dosw.todo.exception.EmailAlreadyExistsException;
import edu.eci.dosw.todo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del registro (HU-SEG-01). El repositorio y el PasswordEncoder
 * son mocks: se prueba solo la lógica del servicio, sin base de datos.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest("Ana Pérez", "ana@example.com", "ContrasenaDemo123!");
    }

    // Simula que la BD asigna el id al guardar
    private void mockSaveAssigningId() {
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
    }

    private UserEntity captureSavedUser() {
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    // --- PRUEBAS DE REGISTRO (register) ---

    @Test
    void register_shouldAssignUserRole() {
        when(passwordEncoder.encode("ContrasenaDemo123!")).thenReturn("$2a$10$hash");
        mockSaveAssigningId();

        UserResponse result = authService.register(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(captureSavedUser().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void register_shouldStoreHashedPassword() {
        when(passwordEncoder.encode("ContrasenaDemo123!")).thenReturn("$2a$10$hash");
        mockSaveAssigningId();

        authService.register(request);

        UserEntity saved = captureSavedUser();
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(saved.getPasswordHash()).isNotEqualTo("ContrasenaDemo123!");
        verify(passwordEncoder, times(1)).encode("ContrasenaDemo123!");
    }

    @Test
    void register_shouldNormalizeEmail() {
        RegisterRequest mixedCase = new RegisterRequest("  Ana Pérez ", "  Ana@Example.COM ", "ContrasenaDemo123!");
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
        mockSaveAssigningId();

        UserResponse result = authService.register(mixedCase);

        assertThat(result.getEmail()).isEqualTo("ana@example.com");
        assertThat(result.getName()).isEqualTo("Ana Pérez");
        verify(userRepository).existsByEmail("ana@example.com");
    }

    @Test
    void register_shouldAssignCreationDate() {
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
        mockSaveAssigningId();

        authService.register(request);

        assertThat(captureSavedUser().getCreatedAt()).isNotNull();
    }

    @Test
    void register_shouldNotSaveWhenEmailExists() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("ana@example.com");

        // Un correo repetido nunca llega a guardarse ni a calcular un hash
        verify(userRepository, never()).saveAndFlush(any(UserEntity.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void register_shouldNotCreateDuplicateWhenUniqueConstraintFails() {
        // Dos registros simultáneos: el segundo choca con la restricción UNIQUE de la BD
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenThrow(new DataIntegrityViolationException("uq_users_email"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }
}
