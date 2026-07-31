package com.fides.sienep.dto;

import com.fides.sienep.model.DiaSemana;
import com.fides.sienep.model.TipoFrecuencia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FrecuenciaRequestDTO {

    // La columna es VARCHAR(50); sin este límite, un texto más largo fallaría
    // recién en el INSERT en vez de devolver un 400 de validación.
    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(max = 50, message = "La descripción no puede superar los 50 caracteres")
    private String descripcion;

    @NotNull(message = "El tipo de frecuencia no puede ser nulo")
    private TipoFrecuencia tipo;

    @Min(value = 1, message = "El intervalo de días debe ser al menos 1")
    @Max(value = 31, message = "El intervalo de días no puede superar 31")
    private Integer intDias;

    private DiaSemana diaSemana;
}