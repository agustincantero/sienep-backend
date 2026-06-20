package com.fides.sienep.mapper;

import com.fides.sienep.dto.CategoriaInstanciaResponseDTO;
import com.fides.sienep.model.CategoriaInstancia;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoriaInstanciaMapper {
    CategoriaInstanciaResponseDTO toDto(CategoriaInstancia entidad);
    List<CategoriaInstanciaResponseDTO> toDtoList(List<CategoriaInstancia> entidades);
}
