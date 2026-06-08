package com.palmar.palmer.api.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PalmerStockSucursalId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String codArticulo;
    private String codSucursal;
}
