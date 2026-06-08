package com.palmar.palmer.api.repository;

import com.palmar.palmer.api.entity.PalmerDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PalmerDetalleRepository extends JpaRepository<PalmerDetalle, String> {

    Optional<PalmerDetalle> findByCodArticulo(String codArticulo);
}
