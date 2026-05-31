package com.fides.sienep.service;

import com.fides.sienep.dto.ItrRequestDTO;
import com.fides.sienep.dto.ItrResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Itr;
import com.fides.sienep.model.ItrCarrera;
import com.fides.sienep.repository.ItrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItrService {

    private final ItrRepository itrRepository;

    @Transactional
    public ItrResponseDTO crear(ItrRequestDTO dto) {

        if (itrRepository.existsByNomItr(dto.getNomItr())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un ITR con ese nombre."
            );
        }

        Itr itr = new Itr(dto.getNomItr());

        return toResponseDTO(
                itrRepository.save(itr)
        );
    }

    @Transactional(readOnly = true)
    public List<ItrResponseDTO> listar() {

        return itrRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItrResponseDTO obtenerPorId(Integer id) {

        return toResponseDTO(
                buscarOFallar(id)
        );
    }

    @Transactional
    public ItrResponseDTO actualizar(Integer id,
                                     ItrRequestDTO dto) {

        Itr itr = buscarOFallar(id);

        itr.setNomItr(dto.getNomItr());

        return toResponseDTO(
                itrRepository.save(itr)
        );
    }

    @Transactional
    public void darDeBaja(Integer id) {

        Itr itr = buscarOFallar(id);

        itr.setEstado(EstadoLogico.INACTIVO);

        itrRepository.save(itr);
    }

    @Transactional
    public void reactivar(Integer id) {

        Itr itr = buscarOFallar(id);

        itr.setEstado(EstadoLogico.ACTIVO);

        itrRepository.save(itr);
    }

    private Itr buscarOFallar(Integer id) {

        return itrRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "ITR no encontrado: " + id
                        ));
    }

    private ItrResponseDTO toResponseDTO(Itr itr) {

        List<String> carreras = new ArrayList<>();

        for (ItrCarrera itrCarrera : itr.getCarreras()) {

            if (itrCarrera.getCarrera() != null) {
                carreras.add(
                        itrCarrera.getCarrera().getNomCarrera()
                );
            }
        }

        return ItrResponseDTO.builder()
                .idItr(itr.getIdItr())
                .nomItr(itr.getNomItr())
                .estado(itr.getEstado())
                .carreras(carreras)
                .createdAt(itr.getCreatedAt())
                .updatedAt(itr.getUpdatedAt())
                .build();
    }
}