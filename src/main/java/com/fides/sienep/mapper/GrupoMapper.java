package com.fides.sienep.mapper;

import com.fides.sienep.dto.GrupoResponseDTO;
import com.fides.sienep.model.Grupo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GrupoMapper {

    @Mapping(target = "idCarrera",  source = "carrera.idCarrera")
    @Mapping(target = "nomCarrera", source = "carrera.nomCarrera")
    GrupoResponseDTO toDto(Grupo entidad);

    List<GrupoResponseDTO> toDtoList(List<Grupo> entidades);
}