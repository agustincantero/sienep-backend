package com.fides.sienep.mapper;

import com.fides.sienep.dto.CarreraResponseDTO;
import com.fides.sienep.dto.ItrSimpleDTO;
import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.ItrCarrera;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CarreraMapper {

    @Mapping(target = "itrs", expression = "java(mapItrsActivos(entidad.getItrs()))")
    CarreraResponseDTO toDto(Carrera entidad);

    List<CarreraResponseDTO> toDtoList(List<Carrera> entidades);

    default List<ItrSimpleDTO> mapItrsActivos(List<ItrCarrera> itrCarreras) {
        if (itrCarreras == null) {
            return null;
        }
        List<ItrSimpleDTO> activos = new ArrayList<>();
        for (ItrCarrera ic : itrCarreras) {
            if (ic.getEstado() == EstadoLogico.ACTIVO && ic.getItr().getEstado() == EstadoLogico.ACTIVO) {
                activos.add(ItrSimpleDTO.builder()
                        .idItr(ic.getItr().getIdItr())
                        .nomItr(ic.getItr().getNomItr())
                        .build());
            }
        }
        return activos;
    }
}
