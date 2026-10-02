package com.fides.sienep.mapper;

import com.fides.sienep.dto.InformeAdjuntoResponseDTO;
import com.fides.sienep.model.InformeAdjunto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InformeAdjuntoMapper {

    @Mapping(target = "idUsuario", source = "estudiante.idUsuario")
    @Mapping(target = "urlDescarga",
            expression = "java(\"/informes-adjuntos/\" + entidad.getIdInforme() + \"/descargar\")")
    InformeAdjuntoResponseDTO toDto(InformeAdjunto entidad);

    List<InformeAdjuntoResponseDTO> toDtoList(List<InformeAdjunto> entidades);

    // Para el propio estudiante (GET /informes-adjuntos/mios): la descarga va por su endpoint, no por el de
    // funcionarios, que exige VER_INFORME. @Named para que MapStruct no lo use al mapear listas (toDtoList).
    @Named("delEstudiante")
    @Mapping(target = "idUsuario", source = "estudiante.idUsuario")
    @Mapping(target = "urlDescarga",
            expression = "java(\"/informes-adjuntos/mios/\" + entidad.getIdInforme() + \"/descargar\")")
    InformeAdjuntoResponseDTO toDtoDelEstudiante(InformeAdjunto entidad);
}