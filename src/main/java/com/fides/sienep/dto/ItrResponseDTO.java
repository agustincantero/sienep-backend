package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "Información completa de un ITR")
public class ItrResponseDTO {

    @Schema(
            description = "Identificador único del ITR",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "1"
    )
    private Integer idItr;

    @Schema(
            description = "Nombre del ITR",
            example = "ITR Norte"
    )
    private String nomItr;

    @Schema(
            description = "Estado lógico del ITR",
            example = "ACTIVO"
    )
    private EstadoLogico estado;

    @Schema(
            description = "Carreras asociadas al ITR"
    )
    private List<String> carreras;

    @Schema(
            description = "Fecha de creación",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "Fecha de última actualización",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private LocalDateTime updatedAt;
}