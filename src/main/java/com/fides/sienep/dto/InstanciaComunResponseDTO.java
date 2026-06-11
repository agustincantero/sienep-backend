package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class InstanciaComunResponseDTO {

    private int codInstancia;
    private String idNegInstancia;
    private String titulo;
    private LocalDateTime fechaHora;
    private String canal;
    private int idEstudiante;
    private String nombreEstudiante;
    private int idFuncionario;
    private String nombreFuncionario;
    private int idCategoria;
    private String nombreCategoria;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}