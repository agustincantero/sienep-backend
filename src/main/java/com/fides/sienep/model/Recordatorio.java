package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recordatorios", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class Recordatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_recordatorio")
    private int codRecordatorio;

    @Column(name = "id_neg_recordatorio", length = 16)
    private String idNegRecordatorio;

    @Column(name = "titulo", length = 150, nullable = false)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false, columnDefinition = "proyecto.tipo_recordatorio")
    private TipoRecordatorio tipo;

    @Column(name = "fec_hora", nullable = false)
    private LocalDateTime fechaHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cod_instancia", nullable = false)
    private Instancia instancia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaRecordatorio categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable", nullable = false)
    private Funcionario responsable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_frecuencia")
    private Frecuencia frecuencia;

    @Column(name = "fec_fin")
    private LocalDate fechaFin;

    @Column(name = "nro_ocurrencias")
    private Integer nroOcurrencias;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_operativo", nullable = false, columnDefinition = "proyecto.estado_operativo")
    private EstadoRecordatorio estadoOperativo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false, columnDefinition = "proyecto.estado_logico")
    private EstadoLogico estado;

    // Ver Carrera.java para el porqué de @CreationTimestamp/@UpdateTimestamp acá.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "event_id_google", length = 1024)
    private String eventIdGoogle;

    // RF23 — evita mandar el email de "vence pronto" más de una vez para el mismo recordatorio.
    @Column(name = "notificado_proximidad", nullable = false)
    private boolean notificadoProximidad = false;

    // RF27 — true una vez que el recordatorio generó su instancia de seguimiento.
    // Evita generar más de una instancia por el mismo recordatorio.
    @Column(name = "cumplido", nullable = false)
    private boolean cumplido = false;

    public Recordatorio(String titulo, TipoRecordatorio tipo, LocalDateTime fechaHora,
                        Instancia instancia, CategoriaRecordatorio categoria, Funcionario responsable) {
        this.titulo = titulo;
        this.tipo = tipo;
        this.fechaHora = fechaHora;
        this.instancia = instancia;
        this.categoria = categoria;
        this.responsable = responsable;
        this.estadoOperativo = EstadoRecordatorio.FUTURO;
        this.estado = EstadoLogico.ACTIVO;
    }
}