package com.fides.sienep.service;

import com.fides.sienep.dto.InvolucradoRequestDTO;
import com.fides.sienep.dto.InvolucradoResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Instancia;
import com.fides.sienep.model.Involucrado;
import com.fides.sienep.repository.InstanciaRepository;
import com.fides.sienep.repository.InvolucradoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvolucradoService {

    private static final Logger log =
            LoggerFactory.getLogger(InvolucradoService.class);

    private final InvolucradoRepository involucradoRepository;
    private final InstanciaRepository instanciaRepository;

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_INSTANCIA')")
    public InvolucradoResponseDTO agregar(int codInstancia, InvolucradoRequestDTO dto) {
        Instancia instancia = buscarInstanciaOFallar(codInstancia);

        java.util.Optional<Involucrado> existente = involucradoRepository
                .findByIdCodInstanciaAndIdNomInvolucrado(codInstancia, dto.getNomInvolucrado());

        if (existente.isPresent()) {
            Involucrado inv = existente.get();
            if (inv.getEstado() == EstadoLogico.ACTIVO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe un involucrado con ese nombre en esta instancia.");
            }
            inv.setEstado(EstadoLogico.ACTIVO);
            log.info("Involucrado reactivado: {} en instancia {}", dto.getNomInvolucrado(), codInstancia);
            return toResponseDTO(involucradoRepository.save(inv));
        }

        Involucrado involucrado = new Involucrado(instancia, dto.getNomInvolucrado());
        log.info("Involucrado agregado: {} en instancia {}", dto.getNomInvolucrado(), codInstancia);
        return toResponseDTO(involucradoRepository.save(involucrado));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public List<InvolucradoResponseDTO> listar(int codInstancia) {
        buscarInstanciaOFallar(codInstancia);
        List<Involucrado> involucrados =
                involucradoRepository.findByIdCodInstanciaAndEstado(
                        codInstancia, EstadoLogico.ACTIVO);
        List<InvolucradoResponseDTO> resultado = new ArrayList<>();
        for (Involucrado inv : involucrados) {
            resultado.add(toResponseDTO(inv));
        }
        return resultado;
    }

    @Transactional
    @PreAuthorize("hasAuthority('BAJA_INSTANCIA')")
    public void darDeBaja(int codInstancia, String nomInvolucrado) {
        Involucrado involucrado = buscarInvolucradoOFallar(
                codInstancia, nomInvolucrado);
        involucrado.setEstado(EstadoLogico.INACTIVO);
        involucradoRepository.save(involucrado);
        log.info("Baja lógica involucrado: {} en instancia {}",
                nomInvolucrado, codInstancia);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Instancia buscarInstanciaOFallar(int codInstancia) {
        return instanciaRepository.findById(codInstancia)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Instancia no encontrada: " + codInstancia));
    }

    private Involucrado buscarInvolucradoOFallar(int codInstancia,
                                                 String nomInvolucrado) {
        return involucradoRepository
                .findByIdCodInstanciaAndIdNomInvolucrado(
                        codInstancia, nomInvolucrado)
                .filter(i -> i.getEstado() == EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Involucrado no encontrado: " + nomInvolucrado));
    }

    private InvolucradoResponseDTO toResponseDTO(Involucrado inv) {
        return InvolucradoResponseDTO.builder()
                .codInstancia(inv.getId().getCodInstancia())
                .nomInvolucrado(inv.getId().getNomInvolucrado())
                .estado(inv.getEstado())
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .build();
    }
}