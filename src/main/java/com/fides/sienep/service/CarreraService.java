package com.fides.sienep.service;

import com.fides.sienep.dto.CarreraRequestDTO;
import com.fides.sienep.dto.CarreraResponseDTO;
import com.fides.sienep.mapper.CarreraMapper;
import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.CarreraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarreraService {

    private final CarreraRepository carreraRepository;
    private final CarreraMapper carreraMapper;

    @PreAuthorize("hasAuthority('CREAR_CARRERA')")
    @Transactional
    public CarreraResponseDTO crear(CarreraRequestDTO dto) {
        if (carreraRepository.existsByNomCarrera(dto.getNomCarrera())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una carrera con ese nombre.");
        }
        Carrera carrera = new Carrera();
        carrera.setNomCarrera(dto.getNomCarrera());
        return carreraMapper.toDto(carreraRepository.save(carrera));
    }

    @PreAuthorize("hasAuthority('VER_CARRERAS')")
    @Transactional(readOnly = true)
    public CarreraResponseDTO obtenerPorId(Integer id) {
        return carreraMapper.toDto(buscarOFallar(id));
    }

    @PreAuthorize("hasAuthority('VER_CARRERAS')")
    @Transactional(readOnly = true)
    public List<CarreraResponseDTO> listar() {
        return carreraMapper.toDtoList(carreraRepository.findAll());
    }

    @PreAuthorize("hasAuthority('EDITAR_CARRERA')")
    @Transactional
    public CarreraResponseDTO actualizar(Integer id, CarreraRequestDTO dto) {
        Carrera carrera = buscarOFallar(id);
        carrera.setNomCarrera(dto.getNomCarrera());
        return carreraMapper.toDto(carreraRepository.save(carrera));
    }

    @PreAuthorize("hasAuthority('ELIMINAR_CARRERA')")
    @Transactional
    public void eliminar(Integer id) {
        Carrera carrera = buscarOFallar(id);
        carrera.setEstado(EstadoLogico.INACTIVO);
        carreraRepository.save(carrera);
    }

    @PreAuthorize("hasAuthority('REACTIVAR_CARRERA')")
    @Transactional
    public void reactivar(Integer id) {
        Carrera carrera = buscarOFallar(id);
        carrera.setEstado(EstadoLogico.ACTIVO);
        carreraRepository.save(carrera);
    }

    private Carrera buscarOFallar(Integer id) {
        return carreraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera no encontrada"));
    }
}