package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "Datos de un informe adjunto de estudiante")
public class InformeAdjuntoResponseDTO {

    private Integer idInforme;
    private Integer idUsuario;
    private String nombre;
    @Schema(description = "Ruta relativa para descargar el archivo (requiere autenticación y permiso VER_INFORME)",
            example = "/informes-adjuntos/1/descargar")
    private String urlDescarga;

    @Schema(description = "Estado actual del informe", example = "ACTIVO")
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
