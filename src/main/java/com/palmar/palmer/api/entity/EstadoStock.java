package com.palmar.palmer.api.entity;

/**
 * Estado de stock de un artículo en una sucursal.
 * El valor de la BD (Oracle CASE) es el nombre en minúsculas.
 */
public enum EstadoStock {
    ok,
    bajo,
    critico
}

