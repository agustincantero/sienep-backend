package com.fides.sienep.mapper;

import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.model.Recordatorio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecordatorioMapper {

    @Mapping(target = "codInstancia", source = "instancia.codInstancia")
    @Mapping(target = "tituloInstancia", source = "instancia.titulo")
    @Mapping(target = "idCategoria", source = "categoria.idCategoria")
    @Mapping(target = "nombreCategoria", source = "categoria.nomCategoria")
    @Mapping(target = "idResponsable", source = "responsable.idUsuario")
    @Mapping(target = "nombreResponsable", expression = "java(entidad.getResponsable().getNombre() + \" \" + entidad.getResponsable().getApellido())")
    @Mapping(target = "idFrecuencia", expression = "java(entidad.getFrecuencia() != null ? entidad.getFrecuencia().getIdFrecuencia() : null)")
    @Mapping(target = "descripcionFrecuencia", expression = "java(entidad.getFrecuencia() != null ? entidad.getFrecuencia().getDescripcion() : null)")
    RecordatorioResponseDTO toDto(Recordatorio entidad);

    List<RecordatorioResponseDTO> toDtoList(List<Recordatorio> entidades);
}