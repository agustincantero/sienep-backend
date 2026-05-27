package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class IncidenciaResponseDTO {

    private int codInstancia;
    private String titulo;
    private LocalDateTime fechaHora;
    private int idEstudiante;
    private String nombreEstudiante;
    private int idFuncionario;
    private String nombreFuncionario;
    private String canal;
    private String idNegInstancia;
    private String lugar;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}