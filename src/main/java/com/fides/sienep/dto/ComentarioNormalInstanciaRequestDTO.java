package com.fides.sienep.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ComentarioNormalInstanciaRequestDTO {

    @NotBlank(message = "El contenido no puede estar vacío")
    private String contenido;
}