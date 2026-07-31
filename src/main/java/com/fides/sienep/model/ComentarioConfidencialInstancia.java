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

        // Sin este @GeneratedValue, Spring Data no puede distinguir un
        // ComentarioConfidencialInstancia nuevo de uno existente (el @EmbeddedId
        // en sí no es null, aunque sus campos sí lo sean), así que save() llama
        // a merge() en vez de persist() y el INSERT nunca llega a ejecutarse —
        // sin lanzar ninguna excepción.
        // Hibernate no soporta GenerationType.IDENTITY dentro de un @EmbeddedId
        // ("Identity generation isn't supported for composite ids"), así que se
        // usa SEQUENCE apuntando directo a la secuencia que ya respalda el SERIAL
        // de la columna (misma secuencia, no una nueva). allocationSize=1 porque
        // esa secuencia también la usa el DEFAULT nextval(...) de la columna a
        // nivel de Postgres — con el allocationSize=50 por defecto de Hibernate,
        // "reservaría" de a 50 ids en memoria y se desincronizaría de cualquier
        // otro insert que no pase por esta misma sesión de Hibernate.
        @Column(name = "id_com_confidencial")
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inst_com_conf_seq")
        @SequenceGenerator(name = "inst_com_conf_seq",
                sequenceName = "proyecto.inst_com_confidenciales_id_com_confidencial_seq",
                allocationSize = 1)
        private Integer idComConfidencial;

        @Column(name = "cod_instancia")
        private Integer codInstancia;
    }
}