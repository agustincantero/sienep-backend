package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para crear o editar una observación confidencial de estudiante")
public class ComentarioConfidencialEstudianteRequestDTO {

    @NotBlank(message = "El contenido es obligatorio")
    @Schema(description = "Contenido de la observación confidencial")
    private String contenido;

    @NotNull(message = "El id del autor es obligatorio")
    @Schema(description = "ID del funcionario autor", example = "1")
    private Integer idAutor;

    @NotNull(message = "El id del estudiante es obligatorio")
    @Schema(description = "ID del estudiante", example = "5")
    private Integer idEstudiante;
}
