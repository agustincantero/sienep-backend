package com.fides.sienep.mapper;

import com.fides.sienep.dto.InformeAdjuntoResponseDTO;
import com.fides.sienep.model.InformeAdjunto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InformeAdjuntoMapper {

    // Un informe ELIMINADO se lista como marca (nombre y fechas) y no lleva urlDescarga: su archivo ya no se
    // puede descargar (el endpoint responde 404).
    @Mapping(target = "idUsuario", source = "estudiante.idUsuario")
    @Mapping(target = "urlDescarga",
            expression = "java(entidad.getEstado() == com.fides.sienep.model.EstadoLogico.ELIMINADO ? null : \"/informes-adjuntos/\" + entidad.getIdInforme() + \"/descargar\")")
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