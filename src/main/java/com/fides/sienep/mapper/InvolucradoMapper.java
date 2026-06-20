package com.fides.sienep.mapper;

import com.fides.sienep.dto.InvolucradoResponseDTO;
import com.fides.sienep.model.Involucrado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InvolucradoMapper {

    @Mapping(target = "codInstancia", source = "id.codInstancia")
    @Mapping(target = "nomInvolucrado", source = "id.nomInvolucrado")
    InvolucradoResponseDTO toDto(Involucrado entidad);

    List<InvolucradoResponseDTO> toDtoList(List<Involucrado> entidades);
}