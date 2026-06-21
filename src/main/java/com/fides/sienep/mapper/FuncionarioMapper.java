package com.fides.sienep.mapper;

import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.model.Funcionario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FuncionarioMapper {

    @Mapping(target = "rol", source = "rol.nombre")
    FuncionarioResponseDTO toDto(Funcionario entidad);

    List<FuncionarioResponseDTO> toDtoList(List<Funcionario> entidades);
}