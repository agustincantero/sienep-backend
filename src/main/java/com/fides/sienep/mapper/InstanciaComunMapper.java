package com.fides.sienep.mapper;

import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.model.InstanciaComun;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InstanciaComunMapper {

    @Mapping(target = "idEstudiante", source = "estudiante.idUsuario")
    @Mapping(target = "nombreEstudiante", expression = "java(entidad.getEstudiante().getNombre() + \" \" + entidad.getEstudiante().getApellido())")
    @Mapping(target = "documentoEstudiante", source = "estudiante.documento")
    @Mapping(target = "idFuncionario", source = "funcionario.idUsuario")
    @Mapping(target = "documentoFuncionario", source = "funcionario.documento")
    @Mapping(target = "nombreFuncionario", expression = "java(entidad.getFuncionario().getNombre() + \" \" + entidad.getFuncionario().getApellido())")
    @Mapping(target = "idCategoria", source = "categoria.idCategoria")
    @Mapping(target = "nombreCategoria", source = "categoria.nomCategoria")
    InstanciaComunResponseDTO toDto(InstanciaComun entidad);

    java.util.List<InstanciaComunResponseDTO> toDtoList(java.util.List<InstanciaComun> entidades);
}