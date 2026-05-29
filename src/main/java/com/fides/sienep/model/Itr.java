package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
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
    @Column(name = "estado")
    private EstadoLogico estado;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}