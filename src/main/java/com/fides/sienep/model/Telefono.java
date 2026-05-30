package com.fides.sienep.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "estu_telefonos", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "estudiante")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Schema(description = "Telefono asociado a un estudiante")
public class Telefono {

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Schema(description = "Clave primaria compuesta: id del estudiante + numero de telefono")
    public static class Id implements Serializable {

        @Column(name = "id_usuario", nullable = false)
        @Schema(description = "ID del estudiante", accessMode = Schema.AccessMode.READ_ONLY)
        private Integer idUsuario;

        @Column(name = "telefono", nullable = false, length = 15)
        @Schema(description = "Numero de telefono", example = "099123456")
        private String numero;
    }

    @EmbeddedId
    @EqualsAndHashCode.Include
    private Id id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idUsuario")
    @JoinColumn(name = "id_usuario", nullable = false)
    @Schema(description = "Estudiante propietario del telefono")
    private Estudiante estudiante;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false)
    @Schema(description = "Estado logico del telefono", example = "ACTIVO")
    private EstadoLogico estado = EstadoLogico.ACTIVO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    public Telefono(Estudiante estudiante, String numero) {
        this.id = new Id(estudiante.getIdUsuario(), numero);
        this.estudiante = estudiante;
        this.estado = EstadoLogico.ACTIVO;
    }
}