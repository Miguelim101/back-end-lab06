package edu.eci.dosw.todo.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import edu.eci.dosw.todo.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * ==========================================================================
 * == LAB 7: Configuración criptográfica del JWT. El encoder (firma) y el  ==
 * ==        decoder (verificación) usan la MISMA llave HMAC HS256, que se ==
 * ==        lee de la variable de entorno JWT_SECRET (nunca del código)   ==
 * ==========================================================================
 */
@Configuration
public class JwtConfig {

    public static final String ROLE_CLAIM = "role";
    public static final String NAME_CLAIM = "name";
    private static final int MIN_SECRET_BYTES = 32; // HS256 exige una llave de al menos 256 bits

    private final SecretKey secretKey;
    private final String issuer;

    public JwtConfig(@Value("${app.jwt.secret}") String secret,
                     @Value("${app.jwt.issuer}") String issuer) {
        this.secretKey = buildSecretKey(secret);
        this.issuer = issuer;
    }

    private static SecretKey buildSecretKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET debe estar en Base64 (generar con: openssl rand -base64 32)");
        }
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos " + MIN_SECRET_BYTES + " bytes");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String getIssuer() { return this.issuer; }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Firma (la verifica el decoder) + exp/nbf + iss + coherencia de sub y role
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                identityValidator()));
        return decoder;
    }

    // El sub debe ser el id numérico del usuario y el rol uno de los definidos en Role
    private static OAuth2TokenValidator<Jwt> identityValidator() {
        return jwt -> {
            String subject = jwt.getSubject();
            String role = jwt.getClaimAsString(ROLE_CLAIM);
            boolean validSubject = subject != null && subject.matches("\\d+");
            boolean validRole = role != null && Arrays.stream(Role.values()).anyMatch(r -> r.name().equals(role));

            if (validSubject && validRole) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "The token subject or role is not valid", null));
        };
    }

    // Claim role ("USER" | "ADMIN") -> autoridad ROLE_USER | ROLE_ADMIN para hasRole(...)
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(ROLE_CLAIM);
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
