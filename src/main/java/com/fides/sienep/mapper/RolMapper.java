package com.fides.sienep.mapper;

import com.fides.sienep.dto.PermisoResumenDTO;
import com.fides.sienep.dto.RolResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Rol;
import com.fides.sienep.model.RolPermiso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RolMapper {

    @Mapping(target = "permisos", source = "rolPermisos")
    RolResponseDTO toDto(Rol entidad);
    
    List<RolResponseDTO> toDtoList(List<Rol> entidades);

    default List<PermisoResumenDTO> mapPermisosActivos(List<RolPermiso> rolPermisos) {
        if (rolPermisos == null) return new ArrayList<>();
        
        List<PermisoResumenDTO> list = new ArrayList<>();
        for (RolPermiso rp : rolPermisos) {
            if (rp.getEstado() == EstadoLogico.ACTIVO) {
                PermisoResumenDTO dto = new PermisoResumenDTO();
                dto.setIdPermiso(rp.getPermiso().getIdPermiso());
                dto.setNombre(rp.getPermiso().getNombre());
                list.add(dto);
            }
        }
        return list;
    }
}
