package com.fides.sienep.service;

import com.fides.sienep.dto.InvolucradoRequestDTO;
import com.fides.sienep.dto.InvolucradoResponseDTO;
import com.fides.sienep.mapper.InvolucradoMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Incidencia;
import com.fides.sienep.model.Involucrado;
import com.fides.sienep.repository.IncidenciaRepository;
import com.fides.sienep.repository.InvolucradoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvolucradoService {

    private final InvolucradoRepository involucradoRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final InvolucradoMapper involucradoMapper;

    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_INCIDENCIA')")
    public InvolucradoResponseDTO agregar(int codInstancia, InvolucradoRequestDTO dto) {
        Incidencia incidencia = buscarIncidenciaOFallar(codInstancia);

        Optional<Involucrado> existente = involucradoRepository
                .findByIdCodInstanciaAndIdNomInvolucrado(codInstancia, dto.getNomInvolucrado());

        if (existente.isPresent()) {
            Involucrado inv = existente.get();
            if (inv.getEstado() == EstadoLogico.ACTIVO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe un involucrado con ese nombre en esta incidencia.");
            }
            inv.setEstado(EstadoLogico.ACTIVO);
            log.info("Involucrado reactivado: {} en incidencia {}", dto.getNomInvolucrado(), codInstancia);
            return involucradoMapper.toDto(involucradoRepository.save(inv));
        }

        Involucrado involucrado = new Involucrado(
                incidenciaRepository.findById(codInstancia).get(), dto.getNomInvolucrado());
        log.info("Involucrado agregado: {} en incidencia {}", dto.getNomInvolucrado(), codInstancia);
        return involucradoMapper.toDto(involucradoRepository.save(involucrado));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INCIDENCIAS')")
    public List<InvolucradoResponseDTO> listar(int codInstancia) {
        buscarIncidenciaOFallar(codInstancia);
        return involucradoMapper.toDtoList(
                involucradoRepository.findByIdCodInstanciaAndEstado(codInstancia, EstadoLogico.ACTIVO));
    }

    @Transactional
    @PreAuthorize("hasAuthority('DESACTIVAR_INCIDENCIA')")
    public void desactivar(int codInstancia, String nomInvolucrado) {
        buscarIncidenciaOFallar(codInstancia);
        Involucrado involucrado = buscarInvolucradoOFallar(codInstancia, nomInvolucrado);
        involucrado.setEstado(EstadoLogico.INACTIVO);
        involucradoRepository.save(involucrado);
        log.info("Baja lógica involucrado: {} en incidencia {}", nomInvolucrado, codInstancia);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Incidencia buscarIncidenciaOFallar(int codInstancia) {
        return incidenciaRepository.findByCodInstanciaAndEstado(codInstancia, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una incidencia activa con el id " + codInstancia));
    }

    private Involucrado buscarInvolucradoOFallar(int codInstancia, String nomInvolucrado) {
        return involucradoRepository
                .findByIdCodInstanciaAndIdNomInvolucrado(codInstancia, nomInvolucrado)
                .filter(i -> i.getEstado() == EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Involucrado no encontrado: " + nomInvolucrado));
    }
}