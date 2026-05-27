package com.fides.sienep.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InstanciaComunRequestDTO {

    @NotNull(message = "Por favor seleccioná la instancia a categorizar")
    private Integer codInstancia;

    @NotNull(message = "Por favor seleccioná la categoría a asignar")
    private Integer idCategoria;
}