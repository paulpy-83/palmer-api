package com.palmar.palmer.api.mapper;

import com.palmar.palmer.api.dto.PalmerStockSucursalDTO;
import com.palmar.palmer.api.entity.PalmerStockSucursal;

public final class PalmerStockSucursalMapper {

    private PalmerStockSucursalMapper() {}

    public static PalmerStockSucursalDTO toDTO(PalmerStockSucursal e) {
        return new PalmerStockSucursalDTO(
                e.getCodArticulo(),
                e.getCodSucursal(),
                e.getSucursalDes(),
                e.getCodEmpresa(),
                e.getCantDispon(),
                e.getCantMinima(),
                e.getCantMaxima(),
                e.getEstadoStock()
        );
    }
}
