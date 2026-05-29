package com.fides.sienep.dto;

import lombok.Setter;

public class FuncionarioRequestDTO {
    @Setter
    private Integer idUsuario;
    private Integer idRol;
    public FuncionarioRequestDTO(Integer idUsuario, Integer idRol) {
        this.idUsuario = idUsuario;
        this.idRol = idRol;
    }
    // getters & setters


    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getIdRol() {
        return idRol;
    }

    public void setIdRol(Integer idRol) {
        this.idRol = idRol;
    }
}
