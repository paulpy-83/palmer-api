package com.palmar.palmer.api.mapper;

import com.palmar.palmer.api.dto.PalmerDashboardDTO;
import com.palmar.palmer.api.entity.PalmerDashboard;

public final class PalmerDashboardMapper {

    private PalmerDashboardMapper() {}

    public static PalmerDashboardDTO toDTO(PalmerDashboard e) {
        return new PalmerDashboardDTO(
                e.getCodArticulo(),
                e.getArticuloDes(),
                e.getCodBarra(),
                e.getCodFamilia(),
                e.getFamiliaDes(),
                e.getCodLinea(),
                e.getLineaDes(),
                e.getCodUnidadMedida(),
                e.getDescUnidadMedida(),
                e.getCantDisponTotal(),
                e.getCantMinima(),
                e.getNroSucursales(),
                e.getNroCrit(),
                e.getNroBajo(),
                e.getPrecioVenta()
        );
    }
}
