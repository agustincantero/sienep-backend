package com.fides.sienep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TelefonoRequestDTO {

    @NotNull(message = "El id del estudiante es obligatorio")
    private Integer idUsuario;

    @NotBlank(message = "El número de teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{8,12}$", message = "El teléfono debe tener entre 8 y 12 dígitos")
    private String numero;
}