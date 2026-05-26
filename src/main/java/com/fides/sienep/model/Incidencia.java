package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "incidencias", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class Incidencia extends Instancia {

    @Column(name = "lugar", length = 50, nullable = false)
    private String lugar;

    public Incidencia(String titulo, LocalDateTime fechaHora, Estudiante estudiante, Funcionario funcionario, String canal, String lugar) {
        super(titulo, fechaHora, estudiante, funcionario, canal);
        this.lugar = lugar;
    }
}