package com.fides.sienep.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InstanciaComunRequestDTO {

    @NotNull(message = "El código de instancia no puede ser nulo")
    private Integer codInstancia;

    @NotNull(message = "El id de categoría no puede ser nulo")
    private Integer idCategoria;
}