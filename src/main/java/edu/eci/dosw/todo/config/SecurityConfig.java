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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ==========================================================================
 * == LAB 7: Reglas de Spring Security. API stateless: no hay sesión HTTP  ==
 * ==        ni formulario de login; solo registro y login son públicos.   ==
 * ==        El resto exige "Authorization: Bearer <JWT>" válido y        ==
 * ==        /admin/** además exige el rol ADMIN (401 vs 403)             ==
 * ==========================================================================
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter,
                                                   JsonAuthenticationEntryPoint authenticationEntryPoint,
                                                   JsonAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // Sin cookies de sesión no hay riesgo CSRF; la identidad viaja en el header Authorization
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults()) // Usa la configuración CORS de WebConfig
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        // /error público para que un 400/404 no termine convertido en 401
                        .requestMatchers("/error").permitAll()
                        // HU-SEG-05: recurso administrativo solo para el rol ADMIN (autoridad ROLE_ADMIN del JWT)
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // HU-SEG-04: el CRUD de tareas (incluido el calendario ?from&to) y todo lo demás exige JWT válido
                        .requestMatchers("/api/v1/tasks/**").authenticated()
                        .anyRequest().authenticated())
                // Valida el Bearer token (firma, exp, iss, sub, role) con el JwtDecoder de JwtConfig
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)   // 401: sin token o token inválido
                        .accessDeniedHandler(accessDeniedHandler));           // 403: token válido sin el rol requerido

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt: hash adaptativo con sal aleatoria por contraseña
        return new BCryptPasswordEncoder();
    }
}
