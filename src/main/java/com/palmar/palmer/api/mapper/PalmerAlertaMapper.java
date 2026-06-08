package com.palmar.palmer.api.mapper;

import com.palmar.palmer.api.dto.PalmerAlertaDTO;
import com.palmar.palmer.api.entity.PalmerAlerta;

public final class PalmerAlertaMapper {

    private PalmerAlertaMapper() {}

    public static PalmerAlertaDTO toDTO(PalmerAlerta e) {
        return new PalmerAlertaDTO(
                e.getCodArticulo(),
                e.getArticuloDes(),
                e.getCodBarra(),
                e.getCodFamilia(),
                e.getFamiliaDes(),
                e.getCodLinea(),
                e.getLineaDes(),
                e.getCodUnidadMedida(),
                e.getDescUnidadMedida(),
                e.getCodSucursal(),
                e.getSucursalDes(),
                e.getCantDispon(),
                e.getCantMinima(),
                e.getEstadoStock(),
                e.getTipoAlerta(),
                e.getPrecioVenta()
        );
    }
}
