package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "Datos de un comentario normal de estudiante")
public class ComentarioNormalEstudianteResponseDTO {

    private Integer id;
    private String contenido;
    private EstadoLogico estado;
    private Integer idEstudiante;
    private Integer idAutor;
    private String nombreAutor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}