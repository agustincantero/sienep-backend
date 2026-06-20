package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ComentarioConfidencialInstanciaResponseDTO {

    private Integer idComConfidencial;
    private Integer codInstancia;
    private String contenido;
    private Integer idAutor;
    private String nombreAutor;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}