package com.fides.sienep.dto;

import com.fides.sienep.model.DiaSemana;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.TipoFrecuencia;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class FrecuenciaResponseDTO {

    private int idFrecuencia;
    private String descripcion;
    private TipoFrecuencia tipo;
    private Integer intDias;
    private DiaSemana diaSemana;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}