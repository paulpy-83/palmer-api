package com.palmar.palmer.api.controller;

import com.palmar.palmer.api.dto.LoginRequestDTO;
import com.palmar.palmer.api.dto.LoginResponseDTO;
import com.palmar.palmer.api.security.JwtUtil;
import com.palmar.palmer.api.security.TokenBlacklistService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO request) {
        log.debug("[AUTH-LOGIN] Intento de login para user={}", request.username());
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        var userDetails = (UserDetails) auth.getPrincipal();
        var token = jwtUtil.generateToken(userDetails);
        var roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        log.debug("[AUTH-LOGIN] Token generado — user={}, jti={}, roles={}",
                userDetails.getUsername(), jwtUtil.extractJti(token), roles);

        return ResponseEntity.ok(new LoginResponseDTO(token, userDetails.getUsername(), roles));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.debug("[AUTH-LOGOUT] Header Authorization: {}", authHeader != null ? "presente" : "ausente");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String username = jwtUtil.extractUsername(token);
                String jti = jwtUtil.extractJti(token);
                var ttl = jwtUtil.extractRemainingTtl(token);
                log.debug("[AUTH-LOGOUT] user={}, jti={}, ttl={}s, ttl.isZero={}", username, jti, ttl.toSeconds(), ttl.isZero());
                if (jti != null && !ttl.isZero()) {
                    log.debug("[AUTH-LOGOUT] Blacklistando jti={}", jti);
                    tokenBlacklistService.blacklist(jti, ttl);
                    log.debug("[AUTH-LOGOUT] Blacklist completado");
                } else {
                    log.debug("[AUTH-LOGOUT] jti null o ttl=0 — no se blacklistea");
                }
            } catch (JwtException e) {
                log.debug("[AUTH-LOGOUT] JwtException — token inválido/expirado: {}", e.getMessage());
            }
        } else {
            log.debug("[AUTH-LOGOUT] Sin Bearer token — logout sin revocar");
        }
        return ResponseEntity.noContent().build();
    }
}
