package com.fides.sienep.mapper;

import com.fides.sienep.dto.InformeAdjuntoResponseDTO;
import com.fides.sienep.model.InformeAdjunto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InformeAdjuntoMapper {

    @Mapping(target = "idUsuario", source = "estudiante.idUsuario")
    @Mapping(target = "urlDescarga",
            expression = "java(\"/informes-adjuntos/\" + entidad.getIdInforme() + \"/descargar\")")
    InformeAdjuntoResponseDTO toDto(InformeAdjunto entidad);

    List<InformeAdjuntoResponseDTO> toDtoList(List<InformeAdjunto> entidades);
}