package com.fides.sienep.mapper;

import com.fides.sienep.dto.TelefonoResponseDTO;
import com.fides.sienep.model.Telefono;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TelefonoMapper {

    @Mapping(target = "idUsuario", source = "id.idUsuario")
    @Mapping(target = "numero",    source = "id.numero")
    TelefonoResponseDTO toDto(Telefono entidad);

    List<TelefonoResponseDTO> toDtoList(List<Telefono> entidades);
}