package com.fides.sienep.mapper;

import com.fides.sienep.dto.IncidenciaResponseDTO;
import com.fides.sienep.model.Incidencia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IncidenciaMapper {

    @Mapping(target = "idEstudiante", source = "estudiante.idUsuario")
    @Mapping(target = "nombreEstudiante", expression = "java(entidad.getEstudiante().getNombre() + \" \" + entidad.getEstudiante().getApellido())")
    @Mapping(target = "documentoEstudiante", source = "estudiante.documento")
    @Mapping(target = "idFuncionario", source = "funcionario.idUsuario")
    @Mapping(target = "documentoFuncionario", source = "funcionario.documento")
    @Mapping(target = "nombreFuncionario", expression = "java(entidad.getFuncionario().getNombre() + \" \" + entidad.getFuncionario().getApellido())")
    IncidenciaResponseDTO toDto(Incidencia entidad);

    List<IncidenciaResponseDTO> toDtoList(List<Incidencia> entidades);
}