package com.fides.sienep.service;

import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.Instancia;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.repository.CategoriaInstanciaRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.repository.InstanciaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstanciaComunService {

    private final InstanciaComunRepository instanciaComunRepository;
    private final InstanciaRepository instanciaRepository;
    private final CategoriaInstanciaRepository categoriaInstanciaRepository;

    public InstanciaComunResponseDTO crear(InstanciaComunRequestDTO dto) {
        if (instanciaComunRepository.existsByCodInstanciaAndIdCategoria(dto.getCodInstancia(), dto.getIdCategoria())) {
            throw new IllegalArgumentException("La instancia " + dto.getCodInstancia() + " ya tiene asignada la categoría " + dto.getIdCategoria());
        }

        Instancia instancia = instanciaRepository.findById(dto.getCodInstancia())
                .orElseThrow(() -> new EntityNotFoundException("Instancia con id " + dto.getCodInstancia() + " no encontrada"));

        CategoriaInstancia categoria = categoriaInstanciaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new EntityNotFoundException("Categoría con id " + dto.getIdCategoria() + " no encontrada"));

        InstanciaComun instanciaComun = new InstanciaComun(instancia, categoria);
        return toResponseDTO(instanciaComunRepository.save(instanciaComun));
    }

    public List<InstanciaComunResponseDTO> listarPorInstancia(int codInstancia) {
        List<InstanciaComun> instanciasComunes = instanciaComunRepository.findByCodInstancia(codInstancia);
        List<InstanciaComunResponseDTO> resultado = new ArrayList<>();
        for (InstanciaComun ic : instanciasComunes) {
            resultado.add(toResponseDTO(ic));
        }
        return resultado;
    }

    public List<InstanciaComunResponseDTO> listarPorCategoria(int idCategoria) {
        List<InstanciaComun> instanciasComunes = instanciaComunRepository.findByIdCategoria(idCategoria);
        List<InstanciaComunResponseDTO> resultado = new ArrayList<>();
        for (InstanciaComun ic : instanciasComunes) {
            resultado.add(toResponseDTO(ic));
        }
        return resultado;
    }

    public void eliminar(int codInstancia, int idCategoria) {
        InstanciaComun.InstanciaComunId id = new InstanciaComun.InstanciaComunId(codInstancia, idCategoria);
        if