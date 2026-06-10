package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO exclusivo para PUT /api/comentarios-normales/{id}
 *
 * ¿Por qué separado del RequestDTO?
 *
 * El RequestDTO (crear) necesita idEstudiante e idAutor para saber
 * a quién le pertenece el comentario y quién lo escribe.
 *
 * El UpdateDTO (editar) NO los necesita:
 *   - El comentario ya existe → ya tiene estudiante y autor asignados.
 *   - Permitir cambiar el idEstudiante o idAutor sería un error de
 *     diseño: estarías "moviendo" el comentario a otro estudiante,
 *     lo cual no tiene sentido de negocio.
 *
 * Principio: un DTO de actualización solo debe contener los campos
 * que SE PUEDEN cambiar, no todos los que existen en la entidad.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para editar un comentario normal de estudiante. Solo se puede modificar el contenido.")
public class ComentarioNormalEstudianteUpdateDTO {

    @NotBlank(message = "El contenido es obligatorio")
    @Schema(
            description = "Nuevo contenido del comentario",
            example = "El estudiante mostró mejoras significativas en la última semana."
    )
    private String contenido;
}