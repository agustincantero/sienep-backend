package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

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
    private TipoAccion accion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = true)
    private Usuario autor;

    @Column(name = "usuario_bd", length = 63)
    private String usuarioBd;

    // Se usa nullable, insertable y updatable en false por que lo gestiona la BD con CURRENT_TIMESTAMP
    @Column(name = "fec_hora", nullable = false, insertable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(length = 45)
    private String ip;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_anterior", columnDefinition = "jsonb")
    private JsonNode valorAnterior;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_nuevo", columnDefinition = "jsonb")
    private JsonNode valorNuevo;

    public Auditoria(String entidad, TipoAccion accion, Usuario autor, String usuarioBd, String ip, JsonNode valorAnterior, JsonNode valorNuevo) {
        this.entidad = entidad;
        this.accion = accion;
        this.autor = autor;
        this.usuarioBd = usuarioBd;
        this.ip = ip;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
    }
}




