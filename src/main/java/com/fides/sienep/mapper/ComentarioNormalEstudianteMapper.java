package com.fides.sienep.mapper;

import com.fides.sienep.dto.ComentarioNormalEstudianteResponseDTO;
import com.fides.sienep.model.ComentarioNormalEstudiante;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ComentarioNormalEstudianteMapper {

    @Mapping(target = "idEstudiante", source = "estudiante.idUsuario")
    @Mapping(target = "idAutor",      source = "autor.idUsuario")
    @Mapping(target = "nombreAutor",  expression = "java(entidad.getAutor().getNombre() + \" \" + entidad.getAutor().getApellido())")
    ComentarioNormalEstudianteResponseDTO toDto(ComentarioNormalEstudiante entidad);

    List<ComentarioNormalEstudianteResponseDTO> toDtoList(List<ComentarioNormalEstudiante> entidades);
}