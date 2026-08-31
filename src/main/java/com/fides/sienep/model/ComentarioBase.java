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

@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
public abstract class ComentarioBase {

    @Column(name = "comentario", nullable = false, columnDefinition = "TEXT")
    protected String contenido;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado")
    protected EstadoLogico estado = EstadoLogico.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_autor", nullable = false)
    protected Funcionario autor;

    // Ver Carrera.java para el porqué de @CreationTimestamp/@UpdateTimestamp acá.
    // Antes de este cambio, updated_at tenía updatable=false: Hibernate no lo
    // tocaba ni siquiera al editar el comentario, dependía 100% del trigger SQL.
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    protected LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    protected LocalDateTime updatedAt;

    public ComentarioBase(String contenido) {
        this.contenido = contenido;
    }
}