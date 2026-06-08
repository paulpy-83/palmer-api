package com.palmar.palmer.api.dto;

import com.palmar.palmer.api.entity.EstadoStock;
import com.palmar.palmer.api.entity.TipoAlerta;

import java.math.BigDecimal;

public record PalmerAlertaDTO(
        String codArticulo,
        String articuloDes,
        String codBarra,
        String codFamilia,
        String familiaDes,
        String codLinea,
        String lineaDes,
        String codUnidadMedida,
        String descUnidadMedida,
        String codSucursal,
        String sucursalDes,
        BigDecimal cantDispon,
        BigDecimal cantMinima,
        EstadoStock estadoStock,
        TipoAlerta tipoAlerta,
        BigDecimal precioVenta
) {}
