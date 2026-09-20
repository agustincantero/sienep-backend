package com.fides.sienep.mapper;

import com.fides.sienep.dto.EstudianteResponseDTO;
import com.fides.sienep.dto.EstudianteResumenDTO;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Grupo;
import com.fides.sienep.model.Telefono;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface EstudianteMapper {

    @Mapping(target = "telefonos", expression = "java(mapTelefonos(entidad.getTelefonos()))")
    @Mapping(target = "grupos",    expression = "java(mapGrupos(entidad.getGrupos()))")
    @Mapping(target = "createdAt", source = "estudianteCreatedAt")
    @Mapping(target = "updatedAt", source = "estudianteUpdatedAt")
    @Mapping(target = "urlFoto",   expression = "java(mapUrlFoto(entidad))")
    EstudianteResponseDTO toDto(Estudiante entidad);

    // urlFoto se mapea siempre; EstudianteService la anula si quien consulta no puede ver la foto
    // (no se decide acá para no acoplar MapStruct a SecurityContextHolder).
    @Mapping(target = "telefonos", expression = "java(mapTelefonos(entidad.getTelefonos()))")
    @Mapping(target = "grupos",    expression = "java(mapGrupos(entidad.getGrupos()))")
    @Mapping(target = "urlFoto",   expression = "java(mapUrlFoto(entidad))")
    EstudianteResumenDTO toResumenDto(Estudiante entidad);

    List<EstudianteResponseDTO> toDtoList(List<Estudiante> entidades);

    List<EstudianteResumenDTO> toResumenDtoList(List<Estudiante> entidades);

    default String mapUrlFoto(Estudiante entidad) {
        return entidad.getFoto() != null ? "/estudiantes/" + entidad.getIdUsuario() + "/foto" : null;
    }

    default List<String> mapTelefonos(List<Telefono> telefonos) {
        if (telefonos == null) return new ArrayList<>();
        List<String> numeros = new ArrayList<>();
        for (Telefono t : telefonos) {
            numeros.add(t.getId().getNumero());
        }
        return numeros;
    }

    default List<String> mapGrupos(List<Grupo> grupos) {
        if (grupos == null) return new ArrayList<>();
        List<String> nombres = new ArrayList<>();
        for (Grupo g : grupos) {
            nombres.add(g.getNomGrupo());
        }
        return nombres;
    }
}