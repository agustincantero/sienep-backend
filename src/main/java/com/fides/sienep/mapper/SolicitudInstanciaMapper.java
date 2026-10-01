package com.fides.sienep.mapper;

import com.fides.sienep.dto.SolicitudInstanciaResponseDTO;
import com.fides.sienep.model.SolicitudInstancia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SolicitudInstanciaMapper {

    // funcionario e instancia son null mientras la solicitud está PENDIENTE: MapStruct ya devuelve null para
    // las rutas anidadas (source = "funcionario.idUsuario"), pero las expressions necesitan el chequeo a mano.
    @Mapping(target = "idEstudiante", source = "estudiante.idUsuario")
    @Mapping(target = "nombreEstudiante", expression = "java(entidad.getEstudiante().getNombre() + \" \" + entidad.getEstudiante().getApellido())")
    @Mapping(target = "idFuncionario", source = "funcionario.idUsuario")
    @Mapping(target = "nombreFuncionario", expression = "java(entidad.getFuncionario() != null ? entidad.getFuncionario().getNombre() + \" \" + entidad.getFuncionario().getApellido() : null)")
    @Mapping(target = "codInstancia", source = "instancia.codInstancia")
    @Mapping(target = "idNegInstancia", source = "instancia.idNegInstancia")
    SolicitudInstanciaResponseDTO toDto(SolicitudInstancia entidad);

    List<SolicitudInstanciaResponseDTO> toDtoList(List<SolicitudInstancia> entidades);
}
