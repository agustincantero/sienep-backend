package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

@Schema(description = "Datos necesarios para crear un funcionario")
public class FuncionarioRequestDTO {

    @NotNull
    private Integer idUsuario;

    @NotNull
    private Integer idRol;
}