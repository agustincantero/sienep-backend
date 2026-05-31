package com.fides.sienep.service;

import com.fides.sienep.dto.ItrCarreraRequestDTO;
import com.fides.sienep.dto.ItrCarreraResponseDTO;
import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Itr;
import com.fides.sienep.model.ItrCarrera;
import com.fides.sienep.repository.CarreraRepository;
import com.fides.sienep.repository.ItrCarreraRepository;
import com.fides.sienep.repository.ItrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItrCarreraService {

    private final ItrCarreraRepository itrCarreraRepository;
    private final ItrRepository itrRepository;
    private final CarreraRepository carreraRepository;

    /* ******************************
                  CREAR
     ******************************/

    @Transactional
    public ItrCarreraResponseDTO crear(ItrCarreraRequestDTO dto) {

        var relacionExistente =
                itrCarreraRepository.findByItrIdItrAndCarreraIdCarrera(
                        dto.getIdItr(),
                        dto.getIdCarrera()
                );

        if (relacionExistente.isPresent()) {

            ItrCarrera relacion = relacionExistente.get();

            if (relacion.getEstado() == EstadoLogico.INACTIVO) {

                relacion.setEstado(EstadoLogico.ACTIVO);

                return toResponseDTO(
                        itrCarreraRepository.save(relacion)
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La asociación ITR-Carrera ya existe."
            );
        }

        Itr itr = itrRepository.findById(dto.getIdItr())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "ITR no encontrado."
                        ));

        Carrera carrera = carreraRepository.findById(dto.getIdCarrera())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Carrera no encontrada."
                        ));

        ItrCarrera relacion = new ItrCarrera(
                itr,
                carrera
        );

        return toResponseDTO(
                itrCarreraRepository.save(relacion)
        );
    }

    /* ******************************
                     LISTAR
      ******************************/

    @Transactional(readOnly = true)
    public List<ItrCarreraResponseDTO> listar() {

        return itrCarreraRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /* ******************************
                 OBTENER
    * ******************************/

    @Transactional(readOnly = true)
    public ItrCarreraResponseDTO obtener(
            Integer idItr,
            Integer idCarrera) {

        return toResponseDTO(
                buscarOFallar(idItr, idCarrera)
        );
    }

    /* ******************************
                 BAJA LÓGICA
    * ******************************/

    @Transactional
    public void eliminar(
            Integer idItr,
            Integer idCarrera) {

        ItrCarrera relacion =
                buscarOFallar(idItr, idCarrera);

        relacion.setEstado(EstadoLogico.INACTIVO);

        itrCarreraRepository.save(relacion);
    }

    /* ******************************
                 REACTIVAR
    * ******************************/

    @Transactional
    public void reactivar(
            Integer idItr,
            Integer idCarrera) {

        ItrCarrera relacion =
                buscarOFallar(idItr, idCarrera);

        relacion.setEstado(EstadoLogico.ACTIVO);

        itrCarreraRepository.save(relacion);
    }

    /* ******************************
                 METODOS PRIVADOS
     * ******************************/

    private ItrCarrera buscarOFallar(
            Integer idItr,
            Integer idCarrera) {

        return itrCarreraRepository
                .findByItrIdItrAndCarreraIdCarrera(
                        idItr,
                        idCarrera
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Relación ITR-Carrera no encontrada."
                        ));
    }

    private ItrCarreraResponseDTO toResponseDTO(
            ItrCarrera relacion) {

        return ItrCarreraResponseDTO.builder()
                .idItr(relacion.getItr().getIdItr())
                .nombreItr(relacion.getItr().getNomItr())
                .idCarrera(relacion.getCarrera().getIdCarrera())
                .nombreCarrera(relacion.getCarrera().getNomCarrera())
                .estado(relacion.getEstado())
                .createdAt(relacion.getCreatedAt())
                .updatedAt(relacion.getUpdatedAt())
                .build();
    }
}