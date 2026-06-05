package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para asociar una carrera a un ITR")
public class ItrCarreraRequestDTO {

    @NotNull
    @Schema(
            description = "ID del ITR",
            example = "1"
    )
    private Integer idItr;

    @NotNull
    @Schema(
            description = "ID de la carrera",
            example = "2"
    )
    private Integer idCarrera;
}