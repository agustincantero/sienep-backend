package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "DTO que representa un Rol con su información completa y permisos activos.")
public class RolResponseDTO {
    
    @Schema(description = "ID autogenerado del rol", example = "1")
    private int idRol;
    
    @Schema(description = "Nombre único del rol", example = "ADMINISTRADOR")
    private String nombre;
    
    @Schema(description = "Descripción del rol", example = "Rol base del sistema con acceso total")
    private String descripcion;
    
    @Schema(description = "Estado actual del rol", example = "ACTIVO")
    private EstadoLogico estado;
    
    @Schema(description = "Lista de permisos actualmente activos asignados a este rol")
    private List<PermisoResumenDTO> permisos;
}
