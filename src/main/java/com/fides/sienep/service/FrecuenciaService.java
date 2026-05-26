package com.fides.sienep.service;

import com.fides.sienep.dto.FrecuenciaRequestDTO;
import com.fides.sienep.dto.FrecuenciaResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Frecuencia;
import com.fides.sienep.repository.FrecuenciaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FrecuenciaService {

    private final FrecuenciaRepository frecuenciaRepository;

    public FrecuenciaResponseDTO crear(FrecuenciaRequestDTO dto) {
        Frecuencia frecuencia = new Frecuencia(
                dto.getDescripcion(),
                dto.getTipo(),
                dto.getIntDias(),
                dto.getDiaSemana()
        );
        return toResponseDTO(frecuenciaRepository.save(frecuencia));
    }

    public List<FrecuenciaResponseDTO> listarActivas() {
        return frecuenciaRepository.findByEstado(EstadoLogico.ACTIVO)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public FrecuenciaResponseDTO buscarPorId(int id) {
        Frecuencia frecuencia = frecuenciaRepository.findByIdFrecuenciaAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new EntityNotFoundException("Frecuencia con id " + id + " no encontrada"));
        return toResponseDTO(frecuencia);
    }

    public FrecuenciaResponseDTO actualizar(int id, FrecuenciaRequestDTO dto) {
        Frecuencia frecuencia = frecuenciaRepository.findByIdFrecuenciaAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new EntityNotFoundException("Frecuencia con id " + id + " no encontrada"));
        frecuencia.setDescripcion(dto.getDescripcion());
        frecuencia.setTipo(dto.getTipo());
        frecuencia.setIntDias(dto.getIntDias());
        frecuencia.setDiaSemana(dto.getDiaSemana());
        return toResponseDTO(frecuenciaRepository.save(frecuencia));
    }

    public void darDeBaja(int id) {
        Frecuencia frecuencia = frecuenciaRepository.findByIdFrecuenciaAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new EntityNotFoundException("Frecuencia con id " + id + " no encontrada"));
        frecuencia.setEstado(EstadoLogico.INACTIVO);
        frecuenciaRepository.save(frecuencia);
    }

    private FrecuenciaResponseDTO toResponseDTO(Frecuencia frecuencia) {
        FrecuenciaResponseDTO dto = new FrecuenciaResponseDTO();
        dto.setIdFrecuencia(frecuencia.getIdFrecuencia());
        dto.setDescripcion(frecuencia.getDescripcion());
        dto.setTipo(frecuencia.getTipo());
        dto.setIntDias(frecuencia.getIntDias());
        dto.setDiaSemana(frecuencia.getDiaSemana());
        dto.setEstado(frecuencia.getEstado());
        dto.setCreatedAt(frecuencia.getCreatedAt());
        dto.setUpdatedAt(frecuencia.getUpdatedAt());
        return dto;
    }
}