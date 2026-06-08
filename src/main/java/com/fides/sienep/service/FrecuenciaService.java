package com.fides.sienep.service;

import com.fides.sienep.dto.FrecuenciaRequestDTO;
import com.fides.sienep.dto.FrecuenciaResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Frecuencia;
import com.fides.sienep.repository.FrecuenciaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FrecuenciaService {

    private final FrecuenciaRepository frecuenciaRepository;

    @Transactional
    public FrecuenciaResponseDTO crear(FrecuenciaRequestDTO dto) {
        validarCoherenciaFrecuencia(dto);
        Frecuencia frecuencia = new Frecuencia(
                dto.getDescripcion(),
                dto.getTipo(),
                dto.getIntDias(),
                dto.getDiaSemana()
        );
        return toResponseDTO(frecuenciaRepository.save(frecuencia));
    }

    @Transactional
    public List<FrecuenciaResponseDTO> listarActivas() {
        List<Frecuencia> frecuencias = frecuenciaRepository.findByEstado(EstadoLogico.ACTIVO);
        List<FrecuenciaResponseDTO> resultado = new ArrayList<>();
        for (Frecuencia frecuencia : frecuencias) {
            resultado.add(toResponseDTO(frecuencia));
        }
        return resultado;
    }

    @Transactional
    public FrecuenciaResponseDTO buscarPorId(int id) {
        return toResponseDTO(buscarActivaOFallar(id));
    }

    @Transactional
    public FrecuenciaResponseDTO actualizar(int id, FrecuenciaRequestDTO dto) {
        validarCoherenciaFrecuencia(dto);
        Frecuencia frecuencia = buscarActivaOFallar(id);
        frecuencia.setDescripcion(dto.getDescripcion());
        frecuencia.setTipo(dto.getTipo());
        frecuencia.setIntDias(dto.getIntDias());
        frecuencia.setDiaSemana(dto.getDiaSemana());
        return toResponseDTO(frecuenciaRepository.save(frecuencia));
    }

    @Transactional
    public void darDeBaja(int id) {
        Frecuencia frecuencia = buscarActivaOFallar(id);
        frecuencia.setEstado(EstadoLogico.INACTIVO);
        frecuenciaRepository.save(frecuencia);
    }

    private Frecuencia buscarActivaOFallar(int id) {
        return frecuenciaRepository.findByIdFrecuenciaAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró una frecuencia activa con el id " + id));
    }

    private void validarCoherenciaFrecuencia(FrecuenciaRequestDTO dto) {
        switch (dto.getTipo()) {
            case DIARIA -> {
                if (dto.getIntDias() != null || dto.getDiaSemana() != null)
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "La frecuencia diaria no requiere intervalo de días ni día de la semana");
            }
            case SEMANAL -> {
                if (dto.getDiaSemana() == null)
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Por favor indicá el día de la semana para la frecuencia semanal");
            }
            case MENSUAL -> {
                if (dto.getIntDias() == null)
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,"Por favor indicá el intervalo de días para la frecuencia mensual");
            }
        }
    }

    private FrecuenciaResponseDTO toResponseDTO(Frecuencia f) {
        return FrecuenciaResponseDTO.builder()
                .idFrecuencia(f.getIdFrecuencia())
                .descripcion(f.getDescripcion())
                .tipo(f.getTipo())
                .intDias(f.getIntDias())
                .diaSemana(f.getDiaSemana())
                .estado(f.getEstado())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}