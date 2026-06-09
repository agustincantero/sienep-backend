package com.fides.sienep.service;

import com.fides.sienep.dto.FrecuenciaRequestDTO;
import com.fides.sienep.dto.FrecuenciaResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Frecuencia;
import com.fides.sienep.repository.FrecuenciaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FrecuenciaService {

    private final FrecuenciaRepository frecuenciaRepository;

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

    public List<FrecuenciaResponseDTO> listarActivas() {
        List<Frecuencia> frecuencias = frecuenciaRepository.findByEstado(EstadoLogico.ACTIVO);
        List<FrecuenciaResponseDTO> resultado = new ArrayList<>();
        for (Frecuencia frecuencia : frecuencias) {
            resultado.add(toResponseDTO(frecuencia));
        }
        return resultado;
    }

    public FrecuenciaResponseDTO buscarPorId(int id) {
        return toResponseDTO(buscarActivaOFallar(id));
    }

    public FrecuenciaResponseDTO actualizar(int id, FrecuenciaRequestDTO dto) {
        validarCoherenciaFrecuencia(dto);
        Frecuencia frecuencia = buscarActivaOFallar(id);
        frecuencia.setDescripcion(dto.getDescripcion());
        frecuencia.setTipo(dto.getTipo());
        frecuencia.setIntDias(dto.getIntDias());
        frecuencia.setDiaSemana(dto.getDiaSemana());
        return toResponseDTO(frecuenciaRepository.save(frecuencia));
    }

    public void darDeBaja(int id) {
        Frecuencia frecuencia = buscarActivaOFallar(id);
        frecuencia.setEstado(EstadoLogico.INACTIVO);
        frecuenciaRepository.save(frecuencia);
    }

    private Frecuencia buscarActivaOFallar(int id) {
        return frecuenciaRepository.findByIdFrecuenciaAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una frecuencia activa con el id " + id));
    }

    private void validarCoherenciaFrecuencia(FrecuenciaRequestDTO dto) {
        switch (dto.getTipo()) {
            case diaria -> {
                if (dto.getIntDias() != null || dto.getDiaSemana() != null)
                    throw new IllegalArgumentException("La frecuencia diaria no requiere intervalo de días ni día de la semana");
            }
            case semanal -> {
                if (dto.getDiaSemana() == null)
                    throw new IllegalArgumentException("Por favor indicá el día de la semana para la frecuencia semanal");
            }
            case mensual -> {
                if (dto.getIntDias() == null)
                    throw new IllegalArgumentException("Por favor indicá el intervalo de días para la frecuencia mensual");
            }
        }
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