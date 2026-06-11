package com.fides.sienep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class InstanciaComunUpdateDTO {

    @NotBlank(message = "Por favor ingresá el título de la instancia")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String titulo;

    @NotNull(message = "Por favor ingresá la fecha y hora de la instancia")
    private LocalDateTime fechaHora;

    @Size(max = 50, message = "El canal no puede superar los 50 caracteres")
    private String canal;

    @NotNull(message = "Por favor seleccioná la categoría de la instancia")
    private Integer idCategoria;
}