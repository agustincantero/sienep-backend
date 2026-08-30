package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "cat_instancias", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class CategoriaInstancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private int idCategoria;

    @Column(name = "nom_cate_instancias", nullable = false, length = 50, unique = true)
    private String nomCategoria;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private EstadoLogico estado;

    // Ver Carrera.java para el porqué de @CreationTimestamp/@UpdateTimestamp acá.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CategoriaInstancia(int idCategoria, String nomCategoria, String descripcion, EstadoLogico estado) {
        this.idCategoria = idCategoria;
        this.nomCategoria = nomCategoria;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public CategoriaInstancia(String nomCategoria) {
        this.nomCategoria = nomCategoria;
        this.estado = EstadoLogico.ACTIVO;
    }
}
