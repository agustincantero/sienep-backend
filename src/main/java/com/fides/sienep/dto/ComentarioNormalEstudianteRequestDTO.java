package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para crear o editar un comentario normal de estudiante")
public class ComentarioNormalEstudianteRequestDTO {

    @NotBlank(message = "El contenido es obligatorio")
    @Schema(description = "Contenido del comentario", example = "El estudiante mostró mejoras significativas.")
    private String contenido;

    @NotNull(message = "El id del autor es obligatorio")
    @Schema(description = "ID del funcionario autor", example = "1")
    private Integer idAutor;

    @NotNull(message = "El id del estudiante es obligatorio")
    @Schema(description = "ID del estudiante comentado", example = "5")
    private Integer idEstudiante;
}
