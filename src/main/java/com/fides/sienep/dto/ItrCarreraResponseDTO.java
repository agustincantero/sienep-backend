package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "Relación entre ITR y Carrera")
public class ItrCarreraResponseDTO {

    @Schema(example = "1")
    private Integer idItr;

    @Schema(example = "ITR Norte")
    private String nombreItr;

    @Schema(example = "2")
    private Integer idCarrera;

    @Schema(example = "Tecnólogo en Informática")
    private String nombreCarrera;

    @Schema(example = "ACTIVO")
    private EstadoLogico estado;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}