package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lo que manda el estudiante al solicitar una instancia. El estudiante sale del token, no del body.
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Solicitud de una instancia hecha por el propio estudiante")
public class SolicitudInstanciaRequestDTO {

    @NotBlank(message = "Contanos brevemente por qué necesitás la instancia")
    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    @Schema(example = "Quisiera hablar sobre cómo organizar los parciales de este semestre.")
    private String motivo;
}
