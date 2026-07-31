package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id") // Ayuda a que el metodo .contains() de las listas funcione bien
@Entity
@Table(name = "itr_carrera", schema = "proyecto")
public class ItrCarrera {

    @EmbeddedId
    private ItrCarreraId id = new ItrCarreraId();

    @ManyToOne
    @MapsId("idItr")
    @JoinColumn(name = "id_itr", nullable = false)
    private Itr itr;

    @ManyToOne
    @MapsId("idCarrera")
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado")
    private EstadoLogico estado = EstadoLogico.ACTIVO;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    public ItrCarrera(Itr itr, Carrera carrera) {
        this.itr = itr;
        this.carrera = carrera;
        this.id = new ItrCarreraId(itr.getIdItr(), carrera.getIdCarrera());
    }

    /* Se necesita esta clase estatica debido a que tiene clave compuesta
     PRIMARY KEY (id_itr, id_carrera) */

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class ItrCarreraId implements Serializable {

        @Column(name = "id_itr")
        private Integer idItr;

        @Column(name = "id_carrera")
        private Integer idCarrera;

        public ItrCarreraId(Integer idItr, Integer idCarrera) {
            this.idItr = idItr;
            this.idCarrera = idCarrera;
        }
    }
}