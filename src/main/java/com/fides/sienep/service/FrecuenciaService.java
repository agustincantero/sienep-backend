package com.fides.sienep.service;

import com.fides.sienep.dto.FrecuenciaRequestDTO;
import com.fides.sienep.dto.FrecuenciaResponseDTO;
import com.fides.sienep.mapper.FrecuenciaMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Frecuencia;
import com.fides.sienep.repository.FrecuenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FrecuenciaService {

    private final FrecuenciaRepository frecuenciaRepository;
    private final FrecuenciaMapper frecuenciaMapper;

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_RECORDATORIO')")
    public FrecuenciaResponseDTO crear(FrecuenciaRequestDTO dto) {
        validarCoherenciaFrecuencia(dto);
        Frecuencia frecuencia = new Frecuencia(
                dto.getDescripcion(),
                dto.getTipo(),
                dto.getIntDias(),
                dto.getDiaSemana()
        );
        return frecuenciaMapper.toDto(frecuenciaRepository.save(frecuencia));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public List<FrecuenciaResponseDTO> listarActivas() {
        return frecuenciaMapper.toDtoList(
                frecuenciaRepository.findByEstado(EstadoLogico.ACTIVO));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public FrecuenciaResponseDTO buscarPorId(int id) {
        return frecuenciaMapper.toDto(buscarActivaOFallar(id));
    }

    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_RECORDATORIO')")
    public FrecuenciaResponseDTO actualizar(int id, FrecuenciaRequestDTO dto) {
        validarCoherenciaFrecuencia(dto);
        Frecuencia frecuencia = buscarActivaOFallar(id);
        frecuencia.setDescripcion(dto.getDescripcion());
        frecuencia.setTipo(dto.getTipo());
        frecuencia.setIntDias(dto.getIntDias());
        frecuencia.setDiaSemana(dto.getDiaSemana());
        return frecuenciaMapper.toDto(frecuenciaRepository.save(frecuencia));
    }

    @Transactional
    @PreAuthorize("hasAuthority('BAJA_RECORDATORIO')")
    public void darDeBaja(int id) {
        Frecuencia frecuencia = buscarActivaOFallar(id);
        frecuencia.setEstado(EstadoLogico.INACTIVO);
        frecuenciaRepository.save(frecuencia);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Frecuencia buscarActivaOFallar(int id) {
        return frecuenciaRepository.findByIdFrecuenciaAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró una frecuencia activa con el id " + id));
    }

    // RecurrenceRuleBuilder es la fuente de verdad de qué campo usa cada tipo:
    // DIARIA/MENSUAL no usan intDias ni diaSemana; SEMANAL usa diaSemana;
    // PERSONALIZADA usa intDias (como INTERVAL sobre FREQ=DAILY).
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
                if (dto.getIntDias() != null || dto.getDiaSemana() != null)
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "La frecuencia mensual no requiere intervalo de días ni día de la semana");
            }
            case PERSONALIZADA -> {
                if (dto.getIntDias() == null)
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Por favor indicá el intervalo de días para la frecuencia personalizada");
            }
        }
    }
}