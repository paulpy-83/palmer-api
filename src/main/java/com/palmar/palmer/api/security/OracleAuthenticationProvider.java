package com.palmar.palmer.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
public class OracleAuthenticationProvider implements AuthenticationProvider {

    @Value("${auth.datasource.url}")
    private String dbUrl;

    @Value("${auth.datasource.connection-timeout:5000}")
    private int connectionTimeoutMs;

    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schema;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName().toUpperCase();
        String password = authentication.getCredentials().toString();

        UsuarioInfo usuario = validateAndLoadUser(username, password);
        List<SimpleGrantedAuthority> authorities = usuario.codGrupo() != null
                ? List.of(new SimpleGrantedAuthority(usuario.codGrupo()))
                : Collections.emptyList();

        log.debug("Autenticación Oracle exitosa — usuario: {} grupo: {}", username, usuario.codGrupo());

        UserDetails userDetails = User.withUsername(usuario.codUsuario())
                .password(usuario.claveAutorizacion())
                .authorities(authorities)
                .build();
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private UsuarioInfo validateAndLoadUser(String username, String password) {
        var props = new Properties();
        props.setProperty("user", username);
        props.setProperty("password", password);
        props.setProperty("oracle.net.CONNECT_TIMEOUT", String.valueOf(connectionTimeoutMs));

        String sql = "SELECT COD_USUARIO, COD_GRUPO, CLAVE_AUTORIZACION" +
                     " FROM " + schema + ".USUARIOS" +
                     " WHERE COD_USUARIO = ? AND ESTADO = 'A'";

        log.debug("Validando credenciales Oracle para usuario: {}", username);

        try (var conn = DriverManager.getConnection(dbUrl, props);
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    log.warn("Usuario Oracle válido pero inactivo o inexistente en USUARIOS: {}", username);
                    throw new DisabledException("Usuario inactivo o sin acceso a la aplicación");
                }
                return new UsuarioInfo(
                        rs.getString("COD_USUARIO"),
                        rs.getString("COD_GRUPO"),
                        rs.getString("CLAVE_AUTORIZACION")
                );
            }
        } catch (DisabledException e) {
            throw e;
        } catch (SQLException e) {
            // ORA-01017: invalid username/password; ORA-28000: account locked
            if (e.getErrorCode() == 1017 || e.getErrorCode() == 28000) {
                log.warn("Fallo de autenticación Oracle — usuario: {} código: {}", username, e.getErrorCode());
                throw new BadCredentialsException("Credenciales inválidas");
            }
            log.error("Error inesperado al autenticar usuario: {} código Oracle: {}", username, e.getErrorCode());
            throw new InternalAuthenticationServiceException("Error al autenticar", e);
        }
    }

    private record UsuarioInfo(String codUsuario, String codGrupo, String claveAutorizacion) {}
}
