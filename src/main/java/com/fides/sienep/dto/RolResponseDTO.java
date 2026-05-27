package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Objeto de transferencia de datos que representa un Rol con sus permisos detallados")
public class RolResponseDTO {
    
    @Schema(description = "ID único del rol generado por la base de datos", example = "6")
    private int idRol;
    
    @Schema(description = "Nombre del rol", example = "COORDINADOR")
    private String nombre;
    
    @Schema(description = "Descripción del rol", example = "Encargado de coordinar las actividades académicas")
    private String descripcion;
    
    @Schema(description = "Estado actual del rol", example = "ACTIVO")
    private EstadoLogico estado;
    
    @Schema(description = "Lista detallada de los permisos que este rol tiene habilitados actualmente")
    private List<PermisoResumenDTO> permisos;
}
