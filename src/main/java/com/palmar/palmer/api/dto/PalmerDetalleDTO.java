package com.palmar.palmer.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PalmerDetalleDTO(
        String codArticulo,
        String articuloDes,
        String codBarra,
        String imagen,
        BigDecimal costoUltimo,
        BigDecimal precioBase,
        String codEmpresa,
        String codMarca,
        String descMarca,
        String codFamilia,
        String familiaDes,
        String codLinea,
        String lineaDes,
        String codUnidadMedida,
        String descUnidadMedida,
        BigDecimal cantDisponTotal,
        BigDecimal cantMinima,
        Integer nroSucursales,
        BigDecimal precioVenta,
        String codPrecioFijo,
        String descListaPrecio,
        LocalDate fecVigencia
) {}
