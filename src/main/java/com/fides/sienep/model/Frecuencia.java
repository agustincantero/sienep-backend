package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "frecuencias", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class Frecuencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_frecuencia")
    private int idFrecuencia;

    @Column(name = "descripcion", length = 50, nullable = false)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false, columnDefinition = "proyecto.tipo_frecuencia")
    private TipoFrecuencia tipo;

    @Column(name = "int_dias")
    private Integer intDias;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "dia_semana", columnDefinition = "proyecto.dia_semana")
    private DiaSemana diaSemana;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false, columnDefinition = "proyecto.estado_logico")
    private EstadoLogico estado;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    public Frecuencia(String descripcion, TipoFrecuencia tipo, Integer intDias, DiaSemana diaSemana) {
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.intDias = intDias;
        this.diaSemana = diaSemana;
        this.estado = EstadoLogico.ACTIVO;
    }
}