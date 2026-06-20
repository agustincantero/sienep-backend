package com.fides.sienep.mapper;

import com.fides.sienep.dto.ComentarioConfidencialInstanciaResponseDTO;
import com.fides.sienep.model.ComentarioConfidencialInstancia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ComentarioConfidencialInstanciaMapper {

    @Mapping(target = "idComConfidencial", source = "id.idComConfidencial")
    @Mapping(target = "codInstancia", source = "id.codInstancia")
    @Mapping(target = "idAutor", source = "autor.idUsuario")
    @Mapping(target = "nombreAutor", expression = "java(entidad.getAutor().getNombre() + \" \" + entidad.getAutor().getApellido())")
    ComentarioConfidencialInstanciaResponseDTO toDto(ComentarioConfidencialInstancia entidad);

    List<ComentarioConfidencialInstanciaResponseDTO> toDtoList(List<ComentarioConfidencialInstancia> entidades);
}