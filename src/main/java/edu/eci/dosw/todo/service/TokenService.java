package edu.eci.dosw.todo.service;

import edu.eci.dosw.todo.config.JwtConfig;
import edu.eci.dosw.todo.dto.TokenResponse;
import edu.eci.dosw.todo.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * ======================================================================================
 * == LAB 7: Emite el JWT firmado (HS256) con identidad (sub), emisor (iss), fecha de  ==
 * ==        emisión (iat), expiración (exp) y rol. No incluye datos sensibles          ==
 * ======================================================================================
 */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration expiration;

    public TokenService(JwtEncoder jwtEncoder,
                        @Value("${app.jwt.issuer}") String issuer,
                        @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public TokenResponse generateToken(UserEntity user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .claim(JwtConfig.ROLE_CLAIM, user.getRole().name())
                .claim(JwtConfig.NAME_CLAIM, user.getName())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, expiration.toSeconds());
    }
}
