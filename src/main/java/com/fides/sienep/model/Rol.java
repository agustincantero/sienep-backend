package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "permisos")
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private int idRol;

    @Column(name = "nom_rol", nullable = false, length = 50, unique = true)
    private String nombre;

    @Column(name = "rol_descripcion", length = 100)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoLogico estado;

    @ManyToMany
    @JoinTable(
            name = "rol_permisos",
            schema = "proyecto",
            joinColumns = @JoinColumn(name = "id_rol"),
            inverseJoinColumns = @JoinColumn(name = "id_permiso"))
    private List<Permiso> permisos = new ArrayList<>();


    public Rol(String nombre, int idRol, List<Permiso> permisos, EstadoLogico estado) {
        this.nombre = nombre;
        this.idRol = idRol;
        this.permisos = permisos != null ? permisos : new ArrayList<>();
        this.estado = estado;
    }

    public Rol(String nombre) {
        this.nombre = nombre;
        this.estado = EstadoLogico.ACTIVO;
        this.permisos = new ArrayList<>();
    }

    public boolean tienePermiso(String nombrePermiso) {
        if (permisos == null) return false;
        for (Permiso p : permisos) {
            if (p.getNombre().equals(nombrePermiso)) return true;
        }
        return false;
    }

    public void agregarPermiso(Permiso permiso) {
        if (permisos == null) permisos = new ArrayList<>();
        permisos.add(permiso);
    }
}
