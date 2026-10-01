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
@Schema(description = "Rechazo de una solicitud de instancia")
public class RechazarSolicitudInstanciaDTO {

    // Obligatoria: es lo que ve el estudiante para entender por qué no se le dio la instancia.
    @NotBlank(message = "Por favor indica el motivo del rechazo para el estudiante")
    @Size(max = 500, message = "La respuesta no puede superar los 500 caracteres")
    private String respuesta;
}
