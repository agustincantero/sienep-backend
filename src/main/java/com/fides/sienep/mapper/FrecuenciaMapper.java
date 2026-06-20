package com.fides.sienep.mapper;

import com.fides.sienep.dto.FrecuenciaResponseDTO;
import com.fides.sienep.model.Frecuencia;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FrecuenciaMapper {

    FrecuenciaResponseDTO toDto(Frecuencia entidad);

    List<FrecuenciaResponseDTO> toDtoList(List<Frecuencia> entidades);
}