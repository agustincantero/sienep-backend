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
    @Mapping(target = "urlFoto",
            expression = "java(entidad.getFoto() != null ? \"/api/estudiantes/\" + entidad.getIdUsuario() + \"/foto\" : null)")
    EstudianteResponseDTO toDto(Estudiante entidad);

    @Mapping(target = "telefonos", expression = "java(mapTelefonos(entidad.getTelefonos()))")
    @Mapping(target = "grupos",    expression = "java(mapGrupos(entidad.getGrupos()))")
    EstudianteResumenDTO toResumenDto(Estudiante entidad);

    List<EstudianteResponseDTO> toDtoList(List<Estudiante> entidades);

    List<EstudianteResumenDTO> toResumenDtoList(List<Estudiante> entidades);

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