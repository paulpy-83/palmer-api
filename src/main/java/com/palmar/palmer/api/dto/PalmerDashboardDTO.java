package com.palmar.palmer.api.dto;

import java.math.BigDecimal;

public record PalmerDashboardDTO(
        String codArticulo,
        String articuloDes,
        String codBarra,
        String codFamilia,
        String familiaDes,
        String codLinea,
        String lineaDes,
        String codUnidadMedida,
        String descUnidadMedida,
        BigDecimal cantDisponTotal,
        BigDecimal cantMinima,
        Integer nroSucursales,
        Integer nroCrit,
        Integer nroBajo,
        BigDecimal precioVenta
) {}
