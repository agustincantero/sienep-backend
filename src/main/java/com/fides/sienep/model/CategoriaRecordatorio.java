package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "cat_recordatorios", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class CategoriaRecordatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private int idCategoria;

    @Column(name = "nom_cate_recordatorios", nullable = false, length = 50, unique = true)
    private String nomCategoria;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoLogico estado;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    public CategoriaRecordatorio(int idCategoria, String nomCategoria, String descripcion, EstadoLogico estado, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idCategoria = idCategoria;
        this.nomCategoria = nomCategoria;
        this.descripcion = descripcion;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public CategoriaRecordatorio(String nomCategoria) {
        this.nomCategoria = nomCategoria;
        this.estado = EstadoLogico.ACTIVO;
    }
}
