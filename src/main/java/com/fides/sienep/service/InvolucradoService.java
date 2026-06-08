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
    public InvolucradoResponseDTO agregar(int codInstancia,
                                          InvolucradoRequestDTO dto) {
        Instancia instancia = buscarInstanciaOFallar(codInstancia);

        // Verificar que no exista ya un involucrado con ese nombre en esa instancia
        involucradoRepository
                .findByIdCodInstanciaAndIdNomInvolucrado(
                        codInstancia, dto.getNomInvolucrado())
                .ifPresent(i -> {
                    if (i.getEstado() == EstadoLogico.ACTIVO) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Ya existe un involucrado con ese nombre en esta instancia.");
                    }
                    // Si estaba inactivo, lo reactivamos
                    i.setEstado(EstadoLogico.ACTIVO);
                    involucradoRepository.save(i);
                    log.info("Involucrado reactivado: {} en instancia {}",
                            dto.getNomInvolucrado(), codInstancia);
                });

        Involucrado involucrado = new Involucrado(instancia, dto.getNomInvolucrado());
        Involucrado guardado = involucradoRepository.save(involucrado);
        log.info("Involucrado agregado: {} en instancia {}",
                dto.getNomInvolucrado(), codInstancia);
        return toResponseDTO(guardado);
    }

    @Transactional(readOnly = true)
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