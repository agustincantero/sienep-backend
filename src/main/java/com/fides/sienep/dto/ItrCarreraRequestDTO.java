package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Asociación entre un ITR y una Carrera")
public class ItrCarreraRequestDTO {

    @NotNull
    @Schema(example = "1")
    private Integer idItr;

    @NotNull
    @Schema(example = "2")
    private Integer idCarrera;
}