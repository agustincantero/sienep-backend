package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "Datos de un grupo académico")
public class GrupoResponseDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Integer idGrupo;

    private String nomGrupo;

    private Integer generacion;

    private EstadoLogico estado;

    @Schema(description = "ID de la carrera a la que pertenece el grupo")
    private Integer idCarrera;

    @Schema(description = "Nombre de la carrera a la que pertenece el grupo")
    private String nomCarrera;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}