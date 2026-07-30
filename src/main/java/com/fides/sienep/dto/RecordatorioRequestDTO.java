package com.fides.sienep.dto;

import com.fides.sienep.model.TipoRecordatorio;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RecordatorioRequestDTO {

    @NotBlank(message = "Por favor ingresá el título del recordatorio")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String titulo;

    @NotNull(message = "Por favor seleccioná el tipo de recordatorio")
    private TipoRecordatorio tipo;

    @NotNull(message = "Por favor ingresá la fecha y hora del recordatorio")
    @Future(message = "La fecha y hora del recordatorio debe ser futura")
    private LocalDateTime fechaHora;

    @NotNull(message = "Por favor seleccioná la instancia asociada")
    private Integer codInstancia;

    @NotNull(message = "Por favor seleccioná la categoría del recordatorio")
    private Integer idCategoria;

    @NotNull(message = "Por favor seleccioná el responsable del recordatorio")
    private Integer idResponsable;

    private Integer idFrecuencia;

    private LocalDate fechaFin;

    @Min(value = 1, message = "El número de ocurrencias debe ser al menos 1")
    @Max(value = 5, message = "El número de ocurrencias no puede superar 5")
    private Integer nroOcurrencias;
}