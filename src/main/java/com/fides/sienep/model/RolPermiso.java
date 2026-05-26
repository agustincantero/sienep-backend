package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "rol_permisos", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class RolPermiso {

    @EmbeddedId
    private RolPermisoId id = new RolPermisoId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idRol")
    @JoinColumn(name = "id_rol")
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idPermiso")
    @JoinColumn(name = "id_permiso")
    private Permiso permiso;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoLogico estado;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    public RolPermiso(Rol rol, Permiso permiso, EstadoLogico estado) {
        this.rol = rol;
        this.permiso = permiso;
        this.id.setIdRol(rol.getIdRol());
        this.id.setIdPermiso(permiso.getIdPermiso());
        this.estado = estado;
    }
}
