package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO exclusivo para PUT /api/comentarios-confidenciales/{id}
 *
 * Mismo razonamiento que ComentarioNormalEstudianteUpdateDTO.
 * En un PUT de edición, idEstudiante e idAutor son irrelevantes
 * porque el comentario ya está asociado a ellos desde su creación.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para editar una observación confidencial de estudiante. Solo se puede modificar el contenido.")
public class ComentarioConfidencialEstudianteUpdateDTO {

    @NotBlank(message = "El contenido es obligatorio")
    @Schema(description = "Nuevo contenido de la observación confidencial", example = "Se actualizó la evaluación tras la reunión con el equipo.")
    private String contenido;
}