package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.config.JwtConfig;
import edu.eci.dosw.todo.dto.TokenResponse;
import edu.eci.dosw.todo.entity.Role;
import edu.eci.dosw.todo.entity.UserEntity;
import edu.eci.dosw.todo.support.JwtTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de emisión del JWT (HU-SEG-02) con el encoder y decoder REALES de JwtConfig:
 * el token generado debe poder verificarse con la misma llave y contener sub, iss, iat, exp y role.
 */
class TokenServiceTest {

    private static final String ISSUER = "todo-api";

    private JwtConfig jwtConfig;
    private TokenService tokenService;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig(JwtTestSupport.TEST_SECRET, ISSUER);
        tokenService = new TokenService(jwtConfig.jwtEncoder(), ISSUER, 30);

        user = new UserEntity();
        user.setId(7L);
        user.setName("Ana Pérez");
        user.setEmail("ana@example.com");
        user.setPasswordHash("$2a$10$hash");
        user.setRole(Role.USER);
    }

    private Jwt decode(TokenResponse token) {
        return jwtConfig.jwtDecoder().decode(token.getAccessToken());
    }

    @Test
    void generateToken_shouldReturnBearerTokenThatExpiresIn1800Seconds() {
        TokenResponse token = tokenService.generateToken(user);

        assertThat(token.getTokenType()).isEqualTo("Bearer");
        assertThat(token.getExpiresIn()).isEqualTo(1800);
        assertThat(token.getAccessToken().split("\\.")).hasSize(3); // header.payload.firma
    }

    @Test
    void generateToken_shouldIncludeIdentityIssuerAndRole() {
        Jwt jwt = decode(tokenService.generateToken(user));

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Ana Pérez");
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    void generateToken_shouldExpireThirtyMinutesAfterIssuedAt() {
        Jwt jwt = decode(tokenService.generateToken(user));

        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void generateToken_shouldNotIncludeSensitiveData() {
        Jwt jwt = decode(tokenService.generateToken(user));

        assertThat(jwt.getClaims()).doesNotContainKeys("password", "passwordHash", "email");
    }

    @Test
    void generateToken_shouldNotBeAcceptedWithAnotherKey() {
        TokenResponse token = tokenService.generateToken(user);

        byte[] otherKey = new byte[32];
        new SecureRandom().nextBytes(otherKey);
        JwtDecoder otherDecoder = new JwtConfig(Base64.getEncoder().encodeToString(otherKey), ISSUER).jwtDecoder();

        assertThatThrownBy(() -> otherDecoder.decode(token.getAccessToken()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void jwtConfig_shouldNotAcceptShortSecret() {
        String shortSecret = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new JwtConfig(shortSecret, ISSUER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
