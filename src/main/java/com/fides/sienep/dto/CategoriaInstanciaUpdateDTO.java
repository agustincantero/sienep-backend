package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO para la actualización total (PUT) de una Categoría de Instancia Común.")
public class CategoriaInstanciaUpdateDTO {

    @Schema(description = "Nombre único de la categoría de instancia", example = "SEGUIMIENTO_MENSUAL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El nombre de la categoría no puede estar vacío")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String nomCategoria;

    @Schema(description = "Descripción detallada del propósito de la instancia", example = "Instancia de control mensual para evaluar el progreso de las adecuaciones curriculares", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
    private String descripcion;
}
