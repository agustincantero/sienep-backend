package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "inst_comunes", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class InstanciaComun extends Instancia {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private CategoriaInstancia categoria;

    @Column(name = "event_id_google", length = 1024)
    private String eventIdGoogle;

    public InstanciaComun(String titulo, LocalDateTime fechaHora,
                          Estudiante estudiante, Funcionario funcionario,
                          String canal, CategoriaInstancia categoria) {
        super(titulo, fechaHora, estudiante, funcionario, canal);
        this.categoria = categoria;
    }
}