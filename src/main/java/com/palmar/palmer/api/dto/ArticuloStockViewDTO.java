package com.palmar.palmer.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ArticuloStockViewDTO(
        String codArticulo,
        String articuloDes,
        String codSucursal,
        String sucursalDes,
        String codEmpresa,
        String codMarca,
        String descMarca,
        String codFamilia,
        String familiaDes,
        String codLinea,
        String lineaDes,
        String codUnidadMedida,
        String descUnidadMedida,
        BigDecimal costoUltimo,
        BigDecimal precioBase,
        String nroLote,
        BigDecimal cantMinima,
        BigDecimal cantDispon,
        String codPrecioFijo,
        String descListaPrecio,
        LocalDate fecVigencia,
        BigDecimal precioVenta,
        String codBarra
) {}
