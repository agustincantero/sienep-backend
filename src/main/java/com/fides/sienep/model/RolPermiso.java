package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "rol_permisos", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class RolPermiso {

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class RolPermisoId implements Serializable {
        @Column(name = "id_rol")
        private int idRol;

        @Column(name = "id_permiso")
        private int idPermiso;
    }

    @EmbeddedId
    private RolPermisoId id = new RolPermisoId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idRol")
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPermiso")
    @JoinColumn(name = "id_permiso", nullable = false)
    private Permiso permiso;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoLogico estado;

    // Ver Carrera.java para el porqué de @CreationTimestamp/@UpdateTimestamp acá.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public RolPermiso(Rol rol, Permiso permiso, EstadoLogico estado) {
        this.rol = rol;
        this.permiso = permiso;
        this.id.setIdRol(rol.getIdRol());
        this.id.setIdPermiso(permiso.getIdPermiso());
        this.estado = estado;
    }
}
