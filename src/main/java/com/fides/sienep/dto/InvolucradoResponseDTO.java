// dto/InvolucradoResponseDTO.java
package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Datos de respuesta de un involucrado")
public class InvolucradoResponseDTO {

    @Schema(description = "Código de la instancia asociada")
    private int codInstancia;

    @Schema(description = "Nombre del involucrado")
    private String nomInvolucrado;

    @Schema(description = "Estado lógico del involucrado")
    private EstadoLogico estado;

    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;

    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;
}