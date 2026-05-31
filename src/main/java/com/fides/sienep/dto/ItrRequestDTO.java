package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para crear o actualizar un ITR")
public class ItrRequestDTO {

    @NotBlank
    @Size(max = 80)
    @Schema(
            description = "Nombre del ITR",
            example = "ITR Norte"
    )
    private String nomItr;
}