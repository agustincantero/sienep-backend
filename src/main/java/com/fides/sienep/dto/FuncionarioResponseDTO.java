package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder

@Schema(description = "Datos de salida de funcionario")
public class FuncionarioResponseDTO {

    private Integer idUsuario;

    private String nombre;

    private String apellido;

    private Integer idRol;

    private String nombreRol;
}