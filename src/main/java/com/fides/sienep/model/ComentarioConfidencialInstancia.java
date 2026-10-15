package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

@Entity
@Table(name = "inst_com_confidenciales", schema = "proyecto")
public class ComentarioConfidencialInstancia
        extends ComentarioBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_com_confidencial")
    private Integer idComConfidencial;

    @ManyToOne
    @JoinColumn(name = "cod_instancia", nullable = false)
    private Instancia instancia;
}
