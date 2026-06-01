package com.palmar.palmer.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Getter
@Entity
@Immutable
@Table(name = "USUARIOS")
public class Usuario {

    @Id
    @Column(name = "COD_USUARIO")
    private String codUsuario;

    @Column(name = "CLAVE_AUTORIZACION")
    private String claveAutorizacion;

    @Column(name = "COD_GRUPO")
    private String codGrupo;

    @Column(name = "ESTADO")
    private String estado;
}
