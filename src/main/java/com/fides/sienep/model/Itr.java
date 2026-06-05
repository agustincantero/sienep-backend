package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "itrs", schema = "proyecto")
public class Itr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_itr")
    private Integer idItr;

    @Column(name = "nom_itr", nullable = false, length = 80)
    private String nomItr;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado")
    private EstadoLogico estado = EstadoLogico.ACTIVO;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "itr", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItrCarrera> carreras = new ArrayList<>();

    public Itr(String nomItr) {
        this.nomItr = nomItr;
        this.estado = EstadoLogico.ACTIVO;
    }

}