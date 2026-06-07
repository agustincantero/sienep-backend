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
    private String urlArchivo;
    private String ruta;
    private String nombre;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
