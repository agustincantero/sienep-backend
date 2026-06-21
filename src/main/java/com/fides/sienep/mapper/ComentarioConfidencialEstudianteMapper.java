package com.fides.sienep.mapper;

import com.fides.sienep.dto.ComentarioConfidencialEstudianteResponseDTO;
import com.fides.sienep.model.ComentarioConfidencialEstudiante;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ComentarioConfidencialEstudianteMapper {

    @Mapping(target = "idEstudiante", source = "estudiante.idUsuario")
    @Mapping(target = "idAutor",      source = "autor.idUsuario")
    @Mapping(target = "nombreAutor",  expression = "java(entidad.getAutor().getNombre() + \" \" + entidad.getAutor().getApellido())")
    ComentarioConfidencialEstudianteResponseDTO toDto(ComentarioConfidencialEstudiante entidad);

    List<ComentarioConfidencialEstudianteResponseDTO> toDtoList(List<ComentarioConfidencialEstudiante> entidades);
}