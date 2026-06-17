package com.fides.sienep.mapper;

import com.fides.sienep.dto.PermisoResponseDTO;
import com.fides.sienep.model.Permiso;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PermisoMapper {
    PermisoResponseDTO toDto(Permiso entidad);
    List<PermisoResponseDTO> toDtoList(List<Permiso> entidades);
}
