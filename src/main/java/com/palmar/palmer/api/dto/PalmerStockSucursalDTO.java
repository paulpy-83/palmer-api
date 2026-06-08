package com.palmar.palmer.api.dto;

import com.palmar.palmer.api.entity.EstadoStock;

import java.math.BigDecimal;

public record PalmerStockSucursalDTO(
        String codArticulo,
        String codSucursal,
        String sucursalDes,
        String codEmpresa,
        BigDecimal cantDispon,
        BigDecimal cantMinima,
        BigDecimal cantMaxima,
        EstadoStock estadoStock
) {}
