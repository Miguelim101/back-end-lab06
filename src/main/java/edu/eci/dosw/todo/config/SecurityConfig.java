package edu.eci.dosw.todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ==========================================================================
 * == LAB 7: Reglas de Spring Security. API stateless: no hay sesión HTTP  ==
 * ==        ni formulario de login; solo registro y login son públicos    ==
 * ==========================================================================
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Sin cookies de sesión no hay riesgo CSRF; la identidad viajará en el header Authorization
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults()) // Usa la configuración CORS de WebConfig
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        // /error público para que un 400/404 no termine convertido en 401
                        .requestMatchers("/error").permitAll()
                        // TEMPORAL: se elimina en HU-SEG-04 (protección del CRUD con JWT)
                        .requestMatchers("/api/v1/tasks/**").permitAll()
                        .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt: hash adaptativo con sal aleatoria por contraseña
        return new BCryptPasswordEncoder();
    }
}
