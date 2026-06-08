package com.palmar.palmer.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authz -> authz
                    // Rutas de API públicas
                    .requestMatchers("/api/auth/**").permitAll()
                    // Rutas de API protegidas
                    .requestMatchers("/api/**").authenticated()
                    // Todo lo demás (estáticos + rutas SPA) es público
                    .anyRequest().permitAll())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(
                Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(OracleAuthenticationProvider oracleProvider) {
        // ProviderManager exclusivo para el login (POST /api/auth/login).
        // Requests autenticados son procesados por JwtAuthFilter desde los claims del JWT,
        // sin consultar la BD ni pasar por este AuthenticationManager.
        return new ProviderManager(oracleProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Claves almacenadas en texto plano en mayúsculas en USUARIOS.CLAVE_AUTORIZACION.
        // Este encoder ya no participa en el login (OracleAuthenticationProvider valida
        // contra el motor Oracle). Se mantiene para compatibilidad con otros componentes
        // de Spring Security que puedan requerirlo.
        // Migrar a BCryptPasswordEncoder cuando se hasheen las claves en la BD.
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence raw) {
                return raw.toString().toUpperCase();
            }

            @Override
            public boolean matches(CharSequence raw, String encoded) {
                return raw.toString().toUpperCase().equals(encoded);
            }
        };
    }
}
