package com.palmar.palmer.api.repository;

import com.palmar.palmer.api.entity.EstadoStock;
import com.palmar.palmer.api.entity.PalmerAlerta;
import com.palmar.palmer.api.entity.PalmerStockSucursalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PalmerAlertaRepository extends JpaRepository<PalmerAlerta, PalmerStockSucursalId> {

    // Todas las alertas ordenadas: primero críticos, luego bajos, luego por sucursal
    @Query("SELECT a FROM PalmerAlerta a " +
           "WHERE a.codArticulo IS NOT NULL " +
           "ORDER BY " +
           "  CASE a.estadoStock WHEN com.palmar.palmer.api.entity.EstadoStock.critico THEN 0 ELSE 1 END ASC, " +
           "  a.codSucursal ASC, " +
           "  a.codArticulo ASC")
    List<PalmerAlerta> findAllOrdered();
}
