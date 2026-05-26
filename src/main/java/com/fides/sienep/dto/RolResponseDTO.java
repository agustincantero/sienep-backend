package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import lombok.Data;

import java.util.List;

@Data
public class RolResponseDTO {
    private int idRol;
    private String nombre;
    private String descripcion;
    private EstadoLogico estado;
    private List<PermisoResumenDTO> permisos;
}
