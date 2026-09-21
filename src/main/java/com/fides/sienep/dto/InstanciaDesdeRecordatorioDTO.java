package com.fides.sienep.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// RF27 — datos que el funcionario completa al generar una instancia de seguimiento
// a partir de un recordatorio cumplido. El estudiante y el título por defecto se
// derivan del recordatorio y de su instancia de origen, no se piden acá.
@Getter
@Setter
@NoArgsConstructor
public class InstanciaDesdeRecordatorioDTO {

    @NotNull(message = "Por favor selecciona la categoría de la instancia")
    private Integer idCategoria;

    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String titulo; // opcional — default: "Seguimiento: " + título del recordatorio

    @Size(max = 50, message = "El canal no puede superar los 50 caracteres")
    private String canal; // opcional
}