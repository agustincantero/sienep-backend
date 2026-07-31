package com.fides.sienep.service;

import com.fides.sienep.dto.FrecuenciaRequestDTO;
import com.fides.sienep.dto.FrecuenciaResponseDTO;
import com.fides.sienep.mapper.FrecuenciaMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Frecuencia;
import com.fides.sienep.repository.FrecuenciaRepository;
import com.fides.sienep.repository.RecordatorioRepository;
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
    private final RecordatorioRepository recordatorioRepository;
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

    // estado=null lista todas, sin filtrar — mismo criterio que CategoriaInstanciaService.obtenerTodas().
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public List<FrecuenciaResponseDTO> listar(EstadoLogico estado) {
        List<Frecuencia> frecuencias = estado != null
                ? frecuenciaRepository.findByEstado(estado)
                : frecuenciaRepository.findAll();
        return frecuenciaMapper.toDtoList(frecuencias);
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

        // Integridad referencial: si se desactiva una frecuencia que todavía usan
        // recordatorios activos, RecurrenceRuleBuilder seguiría generando la RRULE
        // de esos recordatorios a partir de un catálogo dado de baja.
        if (recordatorioRepository.existsByFrecuencia_IdFrecuenciaAndEstado(id, EstadoLogico.ACTIVO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede dar de baja una frecuencia en uso por recordatorios activos");
        }

        frecuencia.setEstado(EstadoLogico.INACTIVO);
        frecuenciaRepository.save(frecuencia);
    }

    // Frecuencia comparte todo el namespace de permisos de Recordatorio (no
    // tiene los suyos propios), de ahí REACTIVAR_RECORDATORIO y no REACTIVAR_FRECUENCIA.
    @Transactional
    @PreAuthorize("hasAuthority('REACTIVAR_RECORDATORIO')")
    public FrecuenciaResponseDTO reactivar(int id) {
        Frecuencia frecuencia = frecuenciaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró una frecuencia con el id " + id));

        if (frecuencia.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La frecuencia ya se encuentra activa");
        }

        frecuencia.setEstado(EstadoLogico.ACTIVO);
        return frecuenciaMapper.toDto(frecuenciaRepository.save(frecuencia));
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