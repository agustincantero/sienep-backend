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
            throw new IllegalArgumentException("La instancia ya tiene asignada esa categoría");
        }

        Instancia instancia = instanciaRepository.findById(dto.getCodInstancia())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una instancia con el id " + dto.getCodInstancia()));

        CategoriaInstancia categoria = categoriaInstanciaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una categoría con el id " + dto.getIdCategoria()));

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
        if (!instanciaComunRepository.existsById(id)) {
            throw new EntityNotFoundException("No se encontró la relación entre la instancia " + codInstancia + " y la categoría " + idCategoria);
        }
        instanciaComunRepository.deleteById(id);
    }

    private InstanciaComunResponseDTO toResponseDTO(InstanciaComun ic) {
        InstanciaComunResponseDTO dto = new InstanciaComunResponseDTO();
        dto.setCodInstancia(ic.getInstancia().getCodInstancia());
        dto.setTituloInstancia(ic.getInstancia().getTitulo());
        dto.setIdCategoria(ic.getCategoria().getIdCategoria());
        dto.setNombreCategoria(ic.getCategoria().getNomCategoria());
        dto.setCreatedAt(ic.getCreatedAt());
        dto.setUpdatedAt(ic.getUpdatedAt());
        return dto;
    }
}