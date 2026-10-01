package com.fides.sienep.mapper;

import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.model.Funcionario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FuncionarioMapper {

    @Mapping(target = "rol", source = "rol.nombre")
    @Mapping(target = "urlFoto", expression = "java(mapUrlFoto(entidad))")
    FuncionarioResponseDTO toDto(Funcionario entidad);

    // Mismo formato que EstudianteMapper.mapUrlFoto: la ruta del endpoint, nunca la ruta física.
    default String mapUrlFoto(Funcionario entidad) {
        return entidad.getFoto() != null ? "/funcionarios/" + entidad.getIdUsuario() + "/foto" : null;
    }

    List<FuncionarioResponseDTO> toDtoList(List<Funcionario> entidades);
}