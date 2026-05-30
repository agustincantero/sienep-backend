package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para crear o actualizar una carrera")
public class CarreraRequestDTO {

    @NotBlank
    @Size(max = 80)
    @Schema(description = "Nombre de la carrera", example = "Licenciatura en Tecnologías de la Información")
    private String nomCarrera;
    /* este es el unico atributo que el cliente esta autorizado a proporcionar o modificar */
}