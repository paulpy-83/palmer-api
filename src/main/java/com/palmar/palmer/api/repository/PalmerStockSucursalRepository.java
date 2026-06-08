package com.palmar.palmer.api.repository;

import com.palmar.palmer.api.entity.PalmerStockSucursal;
import com.palmar.palmer.api.entity.PalmerStockSucursalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PalmerStockSucursalRepository extends JpaRepository<PalmerStockSucursal, PalmerStockSucursalId> {

    List<PalmerStockSucursal> findByCodArticuloOrderByCodSucursalAsc(String codArticulo);
}
