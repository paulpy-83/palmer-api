package com.palmar.palmer.api.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        log.debug("[JWT-FILTER] {} {} — Authorization: {}",
                request.getMethod(), request.getRequestURI(),
                authHeader != null ? "presente" : "ausente");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtUtil.extractValidClaims(token).ifPresentOrElse(claims -> {
                String username = claims.getSubject();
                log.debug("[JWT-FILTER] Username extraído del token: {}", username);
                if (SecurityContextHolder.getContext().getAuthentication() != null) {
                    log.debug("[JWT-FILTER] SecurityContext ya autenticado — skip");
                    return;
                }
                String jti = claims.getId();
                log.debug("[JWT-FILTER] jti extraído: {}", jti);
                if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                    log.debug("[JWT-FILTER] jti={} está en blacklist — request rechazado sin auth", jti);
                    return;
                }
                @SuppressWarnings("unchecked")
                List<String> rolesList = (List<String>) claims.get("roles", List.class);
                List<SimpleGrantedAuthority> authorities = (rolesList != null ? rolesList : Collections.<String>emptyList())
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                var authToken = new UsernamePasswordAuthenticationToken(username, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("[JWT-FILTER] SecurityContext autenticado — user={}, roles={}", username, authorities);
            }, () -> log.debug("[JWT-FILTER] Token inválido o expirado — username no extraído"));
        }

        filterChain.doFilter(request, response);
    }
}
