package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private int id;

    @Column(nullable = false, length = 100)
    private String entidad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private AccionAuditoria accion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario autor;

    // Se usa nullable, insertable y updatable en false por que lo gestiona la BD con CURRENT_TIMESTAMP
    @Column(name = "fec_hora", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    public Auditoria(String entidad, AccionAuditoria accion, Usuario autor, String detalle) {
        this.entidad = entidad;
        this.accion = accion;
        this.autor = autor;
        this.detalle = detalle;
    }
}




