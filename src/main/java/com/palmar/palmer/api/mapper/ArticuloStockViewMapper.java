package com.palmar.palmer.api.mapper;

import com.palmar.palmer.api.dto.ArticuloStockViewDTO;
import com.palmar.palmer.api.entity.ArticuloStockView;

public final class ArticuloStockViewMapper {

    private ArticuloStockViewMapper() {}

    public static ArticuloStockViewDTO toDTO(ArticuloStockView entity) {
        return new ArticuloStockViewDTO(
                entity.getCodArticulo(),
                entity.getArticuloDes(),
                entity.getCodSucursal(),
                entity.getSucursalDes(),
                entity.getCodEmpresa(),
                entity.getCodMarca(),
                entity.getDescMarca(),
                entity.getCodFamilia(),
                entity.getFamiliaDes(),
                entity.getCodLinea(),
                entity.getLineaDes(),
                entity.getCodUnidadMedida(),
                entity.getDescUnidadMedida(),
                entity.getCostoUltimo(),
                entity.getPrecioBase(),
                entity.getNroLote(),
                entity.getCantMinima(),
                entity.getCantDispon(),
                entity.getCodPrecioFijo(),
                entity.getDescListaPrecio(),
                entity.getFecVigencia(),
                entity.getPrecioVenta(),
                entity.getCodBarra()
        );
    }
}
