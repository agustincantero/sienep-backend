package com.fides.sienep.mapper;

import com.fides.sienep.dto.ComentarioNormalInstanciaResponseDTO;
import com.fides.sienep.model.ComentarioNormalInstancia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ComentarioNormalInstanciaMapper {

    @Mapping(target = "codInstancia", source = "instancia.codInstancia")
    @Mapping(target = "idAutor", source = "autor.idUsuario")
    // editado lo calcula el service (ComentarioNormalInstanciaService.aDto), no sale de una columna.
    @Mapping(target = "editado", ignore = true)
    @Mapping(target = "nombreAutor", expression = "java(entidad.getAutor().getNombre() + \" \" + entidad.getAutor().getApellido())")
    ComentarioNormalInstanciaResponseDTO toDto(ComentarioNormalInstancia entidad);

    List<ComentarioNormalInstanciaResponseDTO> toDtoList(List<ComentarioNormalInstancia> entidades);
}