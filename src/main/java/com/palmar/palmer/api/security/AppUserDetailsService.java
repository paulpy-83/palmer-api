package com.palmar.palmer.api.security;

import com.palmar.palmer.api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByCodUsuarioAndEstado(username.toUpperCase(), "A")
                .orElseThrow(() -> new UsernameNotFoundException(username));

        var authorities = usuario.getCodGrupo() != null
                ? List.of(new SimpleGrantedAuthority(usuario.getCodGrupo()))
                : Collections.<SimpleGrantedAuthority>emptyList();

        return User.withUsername(usuario.getCodUsuario())
                .password(usuario.getClaveAutorizacion())
                .authorities(authorities)
                .build();
    }
}
