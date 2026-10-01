package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Datos con los que el funcionario crea la instancia al atender una solicitud. El estudiante sale de la
// solicitud y el funcionario responsable del token (mismo criterio que InstanciaDesdeRecordatorioDTO).
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos de la instancia que se crea al atender una solicitud")
public class AtenderSolicitudInstanciaDTO {

    @NotNull(message = "Por favor selecciona la categoría de la instancia")
    private Integer idCategoria;

    @NotNull(message = "Por favor ingresa la fecha y hora de la instancia")
    private LocalDateTime fechaHora;

    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    @Schema(description = "Opcional. Por defecto: \"Solicitud del estudiante\"")
    private String titulo;

    @Size(max = 50, message = "El canal no puede superar los 50 caracteres")
    private String canal;

    @Size(max = 500, message = "La respuesta no puede superar los 500 caracteres")
    @Schema(description = "Opcional. Mensaje para el estudiante (ej. lugar o indicaciones).")
    private String respuesta;
}
