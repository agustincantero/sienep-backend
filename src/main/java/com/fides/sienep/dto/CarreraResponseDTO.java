package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "Datos de una carrera")
public class CarreraResponseDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Integer idCarrera;

    private String nomCarrera;

    @Schema(description = "Estado actual de la carrera", example = "ACTIVO")
    private EstadoLogico estado;

    private List<ItrSimpleDTO> itrs;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;


}