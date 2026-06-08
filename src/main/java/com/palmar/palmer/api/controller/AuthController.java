package com.palmar.palmer.api.controller;

import com.palmar.palmer.api.dto.LoginRequestDTO;
import com.palmar.palmer.api.dto.LoginResponseDTO;
import com.palmar.palmer.api.security.JwtUtil;
import com.palmar.palmer.api.security.TokenBlacklistService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        var userDetails = (UserDetails) auth.getPrincipal();
        var token = jwtUtil.generateToken(userDetails);
        var roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return ResponseEntity.ok(new LoginResponseDTO(token, userDetails.getUsername(), roles));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String jti = jwtUtil.extractJti(token);
                var ttl = jwtUtil.extractRemainingTtl(token);
                if (jti != null && !ttl.isZero()) {
                    tokenBlacklistService.blacklist(jti, ttl);
                }
            } catch (JwtException ignored) {
                // token inválido o expirado — nada que revocar
            }
        }
        return ResponseEntity.noContent().build();
    }
}
