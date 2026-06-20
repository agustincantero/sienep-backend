package com.fides.sienep.mapper;

import com.fides.sienep.dto.CategoriaRecordatorioResponseDTO;
import com.fides.sienep.model.CategoriaRecordatorio;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoriaRecordatorioMapper {
    CategoriaRecordatorioResponseDTO toDto(CategoriaRecordatorio entidad);
    List<CategoriaRecordatorioResponseDTO> toDtoList(List<CategoriaRecordatorio> entidades);
}
