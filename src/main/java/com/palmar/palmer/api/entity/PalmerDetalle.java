package com.palmar.palmer.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "VW_PALMER_DETALLE")
@Immutable
@Getter
@NoArgsConstructor
public class PalmerDetalle {

    @Id
    @Column(name = "COD_ARTICULO", length = 5)
    private String codArticulo;

    @Column(name = "DESC_ARTICULO", length = 100)
    private String articuloDes;

    @Column(name = "COD_BARRA", length = 50)
    private String codBarra;

    @Column(name = "IMAGEN", length = 255)
    private String imagen;

    @Column(name = "COSTO_ULTIMO")
    private BigDecimal costoUltimo;

    @Column(name = "PRECIO_BASE")
    private BigDecimal precioBase;

    @Column(name = "COD_EMPRESA", length = 5)
    private String codEmpresa;

    @Column(name = "COD_MARCA", length = 5)
    private String codMarca;

    @Column(name = "DESC_MARCA", length = 100)
    private String descMarca;

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

    @Column(name = "PRECIO_VENTA")
    private BigDecimal precioVenta;

    @Column(name = "COD_PRECIO_FIJO", length = 5)
    private String codPrecioFijo;

    @Column(name = "DESC_LISTA_PRECIO", length = 100)
    private String descListaPrecio;

    @Column(name = "FEC_VIGENCIA")
    private LocalDate fecVigencia;
}
