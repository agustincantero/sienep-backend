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
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "carreras", schema = "proyecto")
public class Carrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrera")
    private Integer idCarrera;

    @Column(name = "nom_carrera", nullable = false, length = 80)
    private String nomCarrera;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado")
    private EstadoLogico estado = EstadoLogico.ACTIVO;

    // @CreationTimestamp/@UpdateTimestamp: mismo criterio que Grupo/InformeAdjunto/
    // Telefono (ver esos archivos) — Hibernate setea el valor y el trigger SQL
    // también lo hace, sin conflicto, pero así no depende únicamente del trigger
    // si alguna vez se desactiva o un entorno (ej. H2 de test) no lo replica.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "carrera", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItrCarrera> itrs = new ArrayList<>();

    public Carrera(String nomCarrera) {
        this.nomCarrera = nomCarrera;
        this.estado = EstadoLogico.ACTIVO;
    }
}