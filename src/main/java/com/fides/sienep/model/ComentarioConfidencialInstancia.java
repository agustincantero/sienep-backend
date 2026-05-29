package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor

@Entity
@Table(name = "inst_com_confidenciales", schema = "proyecto")
public class ComentarioConfidencialInstancia
        extends ComentarioBase {

    @EmbeddedId
    private ComentarioConfidencialInstanciaId id;

    @ManyToOne
    @MapsId("codInstancia")
    @JoinColumn(name = "cod_instancia")
    private Instancia instancia;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode

    // Se necesita esta clase estatica debido a que tiene clave compuesta
    // PRIMARY KEY (id_com_confidencial, cod_instancia)
    public static class ComentarioConfidencialInstanciaId
            implements Serializable {

        @Column(name = "id_com_confidencial")
        private Integer idComConfidencial;

        @Column(name = "cod_instancia")
        private Integer codInstancia;
    }
}