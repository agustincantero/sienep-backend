package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
public abstract class ComentarioBase {

    @Column(name = "comentario", nullable = false, columnDefinition = "TEXT")
    protected String contenido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_autor", nullable = false)
    protected Funcionario autor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado")
    protected EstadoLogico estado = EstadoLogico.ACTIVO;

    @Column(name = "created_at",  updatable = false, insertable = false)
    protected LocalDateTime createdAt;

    @Column(name = "updated_at", updatable = false, insertable = false)
    protected LocalDateTime updatedAt;

    public ComentarioBase(String contenido, Funcionario autor) {
        this.contenido = contenido;
        this.autor = autor;
    }
}