package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.dto.LoginRequest;
import edu.eci.dosw.todo.dto.RegisterRequest;
import edu.eci.dosw.todo.dto.TokenResponse;
import edu.eci.dosw.todo.dto.UserResponse;
import edu.eci.dosw.todo.entity.Role;
import edu.eci.dosw.todo.entity.UserEntity;
import edu.eci.dosw.todo.exception.EmailAlreadyExistsException;
import edu.eci.dosw.todo.exception.InvalidCredentialsException;
import edu.eci.dosw.todo.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * ======================================================================================
 * == LAB 7: Lógica de registro y login. Normaliza el correo, impide duplicados,      ==
 * ==        guarda solo el hash BCrypt, asigna siempre el rol USER y emite el JWT     ==
 * ======================================================================================
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // Hash de relleno para que un correo inexistente tarde lo mismo que una contraseña incorrecta
    private String dummyPasswordHash;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        UserEntity user = new UserEntity();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER); // El registro público nunca puede crear un ADMIN
        user.setCreatedAt(LocalDateTime.now());

        try {
            // saveAndFlush para que la restricción UNIQUE de la BD se evalúe aquí (dos registros simultáneos)
            return new UserResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Optional<UserEntity> user = userRepository.findByEmail(normalizeEmail(request.getEmail()));

        if (user.isEmpty()) {
            passwordEncoder.matches(request.getPassword(), dummyPasswordHash());
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(request.getPassword(), user.get().getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokenService.generateToken(user.get());
    }

    private String dummyPasswordHash() {
        if (dummyPasswordHash == null) {
            dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        }
        return dummyPasswordHash;
    }

    // "  Ana@Example.com " y "ana@example.com" son la misma cuenta
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
