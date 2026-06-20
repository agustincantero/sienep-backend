package com.fides.sienep.mapper;

import com.fides.sienep.dto.ItrResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Itr;
import com.fides.sienep.model.ItrCarrera;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ItrMapper {

    @Mapping(target = "carreras", source = "carreras", qualifiedByName = "mapCarrerasActivas")
    ItrResponseDTO toDto(Itr itr);

    List<ItrResponseDTO> toDtoList(List<Itr> itrs);

    @Named("mapCarrerasActivas")
    default List<String> mapCarrerasActivas(List<ItrCarrera> itrCarreras) {
        List<String> nombresCarreras = new ArrayList<>();
        if (itrCarreras != null) {
            for (ItrCarrera itrCarrera : itrCarreras) {
                // Filtramos por estado ACTIVO (baja lógica)
                if (itrCarrera.getEstado() == EstadoLogico.ACTIVO &&
                    itrCarrera.getCarrera() != null &&
                    itrCarrera.getCarrera().getEstado() == EstadoLogico.ACTIVO) {
                    nombresCarreras.add(itrCarrera.getCarrera().getNomCarrera());
                }
            }
        }
        return nombresCarreras;
    }
}
