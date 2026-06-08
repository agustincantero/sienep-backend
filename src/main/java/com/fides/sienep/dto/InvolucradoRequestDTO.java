// dto/InvolucradoRequestDTO.java
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
@Schema(description = "Datos para agregar un involucrado a una instancia")
public class InvolucradoRequestDTO {

    @NotBlank(message = "El nombre del involucrado no puede estar vacío")
    @Size(max = 30, message = "El nombre no puede superar los 30 caracteres")
    @Schema(description = "Nombre del involucrado externo", example = "Juan Pérez")
    private String nomInvolucrado;
}