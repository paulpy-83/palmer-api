package com.palmar.palmer.api.entity;

/**
 * Tipo de alerta generada para un artículo en una sucursal.
 * reposicion: el depósito (05) tiene stock — se puede transferir.
 * compra:     el depósito también está vacío — hay que comprar.
 */
public enum TipoAlerta {
    reposicion,
    compra
}
