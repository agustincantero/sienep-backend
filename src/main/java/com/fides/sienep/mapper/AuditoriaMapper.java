package com.fides.sienep.mapper;

import com.fides.sienep.dto.AuditoriaResponseDTO;
import com.fides.sienep.model.Auditoria;
import com.fides.sienep.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditoriaMapper {

    @Mapping(target = "idAuditoria", source = "id")
    @Mapping(target = "idAutor", source = "autor.idUsuario")
    @Mapping(target = "nombreAutor", expression = "java(obtenerNombreCompleto(entidad.getAutor()))")
    @Mapping(target = "valorAnterior", expression = "java(entidad.getValorAnterior() != null ? entidad.getValorAnterior().toString() : null)")
    @Mapping(target = "valorNuevo", expression = "java(entidad.getValorNuevo() != null ? entidad.getValorNuevo().toString() : null)")
    AuditoriaResponseDTO toDto(Auditoria entidad);

    List<AuditoriaResponseDTO> toDtoList(List<Auditoria> entidades);

    default String obtenerNombreCompleto(Usuario autor) {
        if (autor == null) return null;
        return autor.getNombre() + " " + autor.getApellido();
    }
}
