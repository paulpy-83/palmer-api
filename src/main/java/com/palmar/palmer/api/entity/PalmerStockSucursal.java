package com.palmar.palmer.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Table(name = "VW_PALMER_STOCK_SUCURSAL")
@Immutable
@Getter
@NoArgsConstructor
@IdClass(PalmerStockSucursalId.class)
public class PalmerStockSucursal {

    @Id
    @Column(name = "COD_ARTICULO", length = 5)
    private String codArticulo;

    @Id
    @Column(name = "COD_SUCURSAL", length = 5)
    private String codSucursal;

    @Column(name = "DESC_SUCURSAL", length = 100)
    private String sucursalDes;

    @Column(name = "COD_EMPRESA", length = 5)
    private String codEmpresa;

    @Column(name = "CANT_DISPON")
    private BigDecimal cantDispon;

    @Column(name = "CANT_MINIMA")
    private BigDecimal cantMinima;

    @Column(name = "CANT_MAXIMA")
    private BigDecimal cantMaxima;

    @Column(name = "ESTADO_STOCK", length = 10)
    @Enumerated(EnumType.STRING)
    private EstadoStock estadoStock;
}
