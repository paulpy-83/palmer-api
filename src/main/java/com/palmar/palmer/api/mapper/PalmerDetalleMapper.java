package com.palmar.palmer.api.mapper;

import com.palmar.palmer.api.dto.PalmerDetalleDTO;
import com.palmar.palmer.api.entity.PalmerDetalle;

public final class PalmerDetalleMapper {

    private PalmerDetalleMapper() {}

    public static PalmerDetalleDTO toDTO(PalmerDetalle e) {
        return new PalmerDetalleDTO(
                e.getCodArticulo(),
                e.getArticuloDes(),
                e.getCodBarra(),
                e.getImagen(),
                e.getCostoUltimo(),
                e.getPrecioBase(),
                e.getCodEmpresa(),
                e.getCodMarca(),
                e.getDescMarca(),
                e.getCodFamilia(),
                e.getFamiliaDes(),
                e.getCodLinea(),
                e.getLineaDes(),
                e.getCodUnidadMedida(),
                e.getDescUnidadMedida(),
                e.getCantDisponTotal(),
                e.getCantMinima(),
                e.getNroSucursales(),
                e.getPrecioVenta(),
                e.getCodPrecioFijo(),
                e.getDescListaPrecio(),
                e.getFecVigencia()
        );
    }
}
