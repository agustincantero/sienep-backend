package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Nuevo estado a aplicar sobre el recurso")
public class CambiarEstadoDTO {

    @NotNull
    @Schema(description = "Estado destino", example = "ACTIVO")
    private EstadoLogico estado;
}