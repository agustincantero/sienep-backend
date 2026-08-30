package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "instancias", schema = "proyecto")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public abstract class Instancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cod_instancia")
    protected int codInstancia;

    @Column(name = "titulo", length = 150, nullable = false)
    protected String titulo;

    @Column(name = "fec_hora", nullable = false)
    protected LocalDateTime fechaHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    protected Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario", nullable = false)
    protected Funcionario funcionario;

    @Column(name = "id_neg_instancia", length = 16)
    protected String idNegInstancia;

    @Column(name = "canal", length = 50)
    protected String canal;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false, columnDefinition = "proyecto.estado_logico")
    protected EstadoLogico estado;

    // Ver Carrera.java para el porqué de @CreationTimestamp/@UpdateTimestamp acá.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    protected LocalDateTime updatedAt;

    public Instancia(String titulo, LocalDateTime fechaHora, Estudiante estudiante, Funcionario funcionario, String canal) {
        this.titulo = titulo;
        this.fechaHora = fechaHora;
        this.estudiante = estudiante;
        this.funcionario = funcionario;
        this.canal = canal;
        this.estado = EstadoLogico.ACTIVO;
    }

}