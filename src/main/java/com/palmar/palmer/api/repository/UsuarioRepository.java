package com.palmar.palmer.api.repository;

import com.palmar.palmer.api.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByCodUsuarioAndEstado(String codUsuario, String estado);
}
