package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Relación ITR-Carrera")
public class ItrCarreraResponseDTO {

    private Integer idItr;

    private String nombreItr;

    private Integer idCarrera;

    private String nombreCarrera;

    private EstadoLogico estado;
}