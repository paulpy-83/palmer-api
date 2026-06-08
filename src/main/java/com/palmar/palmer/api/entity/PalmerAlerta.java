package com.palmar.palmer.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Table(name = "VW_PALMER_ALERTAS")
@Immutable
@Getter
@NoArgsConstructor
@IdClass(PalmerStockSucursalId.class)
public class PalmerAlerta {

    @Id
    @Column(name = "COD_ARTICULO", length = 5)
    private String codArticulo;

    @Column(name = "DESC_ARTICULO", length = 100)
    private String articuloDes;

    @Column(name = "COD_BARRA", length = 50)
    private String codBarra;

    @Column(name = "COD_FAMILIA", length = 5)
    private String codFamilia;

    @Column(name = "DESC_FAMILIA", length = 100)
    private String familiaDes;

    @Column(name = "COD_LINEA", length = 5)
    private String codLinea;

    @Column(name = "DESC_LINEA", length = 100)
    private String lineaDes;

    @Column(name = "COD_UNIDAD_MEDIDA", length = 5)
    private String codUnidadMedida;

    @Column(name = "DESC_UNIDAD_MEDIDA", length = 100)
    private String descUnidadMedida;

    @Id
    @Column(name = "COD_SUCURSAL", length = 5)
    private String codSucursal;

    @Column(name = "DESC_SUCURSAL", length = 100)
    private String sucursalDes;

    @Column(name = "CANT_DISPON")
    private BigDecimal cantDispon;

    @Column(name = "CANT_MINIMA")
    private BigDecimal cantMinima;

    @Column(name = "ESTADO_STOCK", length = 10)
    @Enumerated(EnumType.STRING)
    private EstadoStock estadoStock;

    @Column(name = "TIPO_ALERTA", length = 15)
    @Enumerated(EnumType.STRING)
    private TipoAlerta tipoAlerta;

    @Column(name = "PRECIO_VENTA")
    private BigDecimal precioVenta;
}
