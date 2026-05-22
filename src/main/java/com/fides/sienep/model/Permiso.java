package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permisos", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private int idPermiso;

    @Column(name = "nom_permiso", nullable = false, length = 50, unique = true)
    private String nombre;

    @Column(name = "per_descripcion", nullable = false, length = 100)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoLogico estado;

    public Permiso(String nombre, int idPermiso, String descripcion) {
        this.nombre = nombre;
        this.idPermiso = idPermiso;
        this.descripcion = descripcion;
        this.estado = EstadoLogico.ACTIVO;
    }

    public Permiso(String nombre, int idPermiso, String descripcion, EstadoLogico estado) {
        this.nombre = nombre;
        this.idPermiso = idPermiso;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public Permiso(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = EstadoLogico.ACTIVO;
    }
}
