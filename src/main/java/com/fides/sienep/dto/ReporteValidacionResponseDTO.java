package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "Resultado de la validación de autenticidad de un reporte PDF")
public class ReporteValidacionResponseDTO {

    @Schema(description = "Indica si el archivo subido coincide exactamente con un PDF generado por SIENEP")
    private boolean autentico;

    @Schema(description = "Fecha y hora en que SIENEP generó el reporte original (null si no es auténtico)")
    private LocalDateTime fechaGeneracion;
}
