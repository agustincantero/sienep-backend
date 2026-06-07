package com.fides.sienep.service;

import com.fides.sienep.dto.CarreraRequestDTO;
import com.fides.sienep.dto.CarreraResponseDTO;
import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.CarreraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarreraService {

    private final CarreraRepository carreraRepository;

    @Transactional
    public CarreraResponseDTO crear(CarreraRequestDTO dto) {

        if (carreraRepository.existsByNomCarrera(dto.getNomCarrera())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una carrera con ese nombre."
            );
        }

        Carrera carrera = new Carrera();
        carrera.setNomCarrera(dto.getNomCarrera());

        return toResponseDTO(
                carreraRepository.save(carrera)
        );
    }

    @Transactional(readOnly = true)
    public CarreraResponseDTO obtenerPorId(Integer id) {

        return toResponseDTO(
                buscarOFallar(id)
        );
    }

    // NO USADO NI IMPLEMENTADO EN SERVICE NI EN CONTROLLER
    @Transactional(readOnly = true)
    public List<CarreraResponseDTO> listar() {
        List<Carrera> carreras = carreraRepository.findAll();
        List<CarreraResponseDTO> dtos = new java.util.ArrayList<>();
        for (Carrera carrera : carreras) {
            dtos.add(toResponseDTO(carrera));
        }
        return dtos;
    }

    @Transactional
    public CarreraResponseDTO actualizar(
            Integer id,
            CarreraRequestDTO dto
    ) {

        Carrera carrera = buscarOFallar(id);

        carrera.setNomCarrera(dto.getNomCarrera());

        return toResponseDTO(
                carreraRepository.save(carrera)
        );
    }

    @Transactional
    public void eliminar(Integer id) {

        Carrera carrera = buscarOFallar(id);

        carrera.setEstado(EstadoLogico.INACTIVO);

        carreraRepository.save(carrera);
    }

    // NO USADO NI IMPLEMENTADO EN SERVICE NI EN CONTROLLER
    @Transactional
    public void reactivar(Integer id) {

        Carrera carrera = buscarOFallar(id);

        carrera.setEstado(
                EstadoLogico.ACTIVO
        );

        carreraRepository.save(carrera);
    }

    private Carrera buscarOFallar(Integer id) {

        return carreraRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Carrera no encontrada"
                        ));
    }

    private CarreraResponseDTO toResponseDTO(Carrera carrera) {

        return CarreraResponseDTO.builder()
                .idCarrera(carrera.getIdCarrera())
                .nomCarrera(carrera.getNomCarrera())
                .estado(carrera.getEstado())
                .createdAt(carrera.getCreatedAt())
                .updatedAt(carrera.getUpdatedAt())
                .build();
    }
}