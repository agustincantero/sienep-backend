package com.fides.sienep.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FuncionarioRequestDTO {

    private String nombre;

    private String apellido;

    private String documento;

    private String email;

    private Integer idRol;
}