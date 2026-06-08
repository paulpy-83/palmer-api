package com.palmar.palmer.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Table(name = "VW_PALMER_DASHBOARD")
@Immutable
@Getter
@NoArgsConstructor
public class PalmerDashboard {

    @Id
    @Column(name = "COD_ARTICULO", length = 5)
    private String codArticulo;

    @Formula("TO_NUMBER(COD_ARTICULO)")
    private Long codArticuloNumero;

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

    @Column(name = "CANT_DISPON_TOTAL")
    private BigDecimal cantDisponTotal;

    @Column(name = "CANT_MINIMA")
    private BigDecimal cantMinima;

    @Column(name = "NRO_SUCURSALES")
    private Integer nroSucursales;

    @Column(name = "NRO_CRIT")
    private Integer nroCrit;

    @Column(name = "NRO_BAJO")
    private Integer nroBajo;

    @Column(name = "PRECIO_VENTA")
    private BigDecimal precioVenta;
}
