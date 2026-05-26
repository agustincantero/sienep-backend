package com.fides.sienep.dto;

import com.fides.sienep.model.DiaSemana;
import com.fides.sienep.model.TipoFrecuencia;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FrecuenciaRequestDTO {

    @NotBlank(message = "La descripción no puede estar vacía")
    private String descripcion;

    @NotNull(message = "El tipo de frecuencia no puede ser nulo")
    private TipoFrecuencia tipo;

    @Min(value = 1, message = "El intervalo de días debe ser al menos 1")
    @Max(value = 31, message = "El intervalo de días no puede superar 31")
    private Integer intDias;

    private DiaSemana diaSemana;
}