package com.fides.sienep.mapper;

import com.fides.sienep.dto.ComentarioConfidencialInstanciaResponseDTO;
import com.fides.sienep.model.ComentarioConfidencialInstancia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ComentarioConfidencialInstanciaMapper {

    @Mapping(target = "codInstancia", source = "instancia.codInstancia")
    @Mapping(target = "idAutor", source = "autor.idUsuario")
    // editado lo calcula el service (ComentarioConfidencialInstanciaService.aDto), no sale de una columna.
    @Mapping(target = "editado", ignore = true)
    @Mapping(target = "nombreAutor", expression = "java(entidad.getAutor().getNombre() + \" \" + entidad.getAutor().getApellido())")
    ComentarioConfidencialInstanciaResponseDTO toDto(ComentarioConfidencialInstancia entidad);

    List<ComentarioConfidencialInstanciaResponseDTO> toDtoList(List<ComentarioConfidencialInstancia> entidades);
}