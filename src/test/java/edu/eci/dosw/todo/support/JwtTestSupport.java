package edu.eci.dosw.todo.support;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Llave JWT SOLO para pruebas: se genera aleatoriamente en cada ejecución,
 * así ninguna llave de firma queda escrita en el repositorio (regla 2.1.10).
 */
public final class JwtTestSupport {

    public static final String TEST_SECRET = randomSecret();

    private JwtTestSupport() { }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static void registerJwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> TEST_SECRET);
    }
}
