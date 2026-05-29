package com.fides.sienep.service;

import com.fides.sienep.dto.CategoriaInstanciaCreateDTO;
import com.fides.sienep.dto.CategoriaInstanciaResponseDTO;
import com.fides.sienep.dto.CategoriaInstanciaUpdateDTO;
import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.CategoriaInstanciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoriaInstanciaService {

    private final CategoriaInstanciaRepository categoriaRepository;

    @Autowired
    public CategoriaInstanciaService(CategoriaInstanciaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    // --- Mapeos Manuales ---
    private CategoriaInstanciaResponseDTO toResponseDTO(CategoriaInstancia categoria) {
        CategoriaInstanciaResponseDTO dto = new CategoriaInstanciaResponseDTO();
        dto.setIdCategoria(categoria.getIdCategoria());
        dto.setNomCategoria(categoria.getNomCategoria());
        dto.setDescripcion(categoria.getDescripcion());
        dto.setEstado(categoria.getEstado());
        return dto;
    }

    // --- Métodos CRUD ---
    @Transactional(readOnly = true)
    public List<CategoriaInstanciaResponseDTO> obtenerTodas(String nombre, EstadoLogico estado) {
        List<CategoriaInstancia> categorias = categoriaRepository.buscarConFiltros(nombre, estado);
        
        List<CategoriaInstanciaResponseDTO> dtoList = new ArrayList<>();
        for (CategoriaInstancia cat : categorias) {
            dtoList.add(toResponseDTO(cat));
        }
        return dtoList;
    }

    @Transactional(readOnly = true)
    public CategoriaInstanciaResponseDTO obtenerPorId(int id) {
        CategoriaInstancia categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría de instancia no encontrada"));
        return toResponseDTO(categoria);
    }

    @Transactional
    public CategoriaInstanciaResponseDTO crearCategoria(CategoriaInstanciaCreateDTO requestDTO) {
        // Normalización del nombre
        String nombreNormalizado = requestDTO.getNomCategoria().trim().toUpperCase().replaceAll("\\s+", "_");

        if (categoriaRepository.existsByNomCategoria(nombreNormalizado)) {
            throw new RuntimeException("El nombre de la categoría ya existe");
        }

        CategoriaInstancia categoria = new CategoriaInstancia();
        categoria.setNomCategoria(nombreNormalizado);
        categoria.setDescripcion(requestDTO.getDescripcion());
        categoria.setEstado(EstadoLogico.ACTIVO); // Forzamos ACTIVO al crear

        CategoriaInstancia guardada = categoriaRepository.save(categoria);
        return toResponseDTO(guardada);
    }

    @Transactional
    public CategoriaInstanciaResponseDTO actualizarCategoria(int id, CategoriaInstanciaUpdateDTO requestDTO) {
        CategoriaInstancia categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría de instancia no encontrada"));

        // Normalización del nombre
        String nombreNormalizado = requestDTO.getNomCategoria().trim().toUpperCase().replaceAll("\\s+", "_");

        // Validar nombre único si lo están cambiando
        if (!categoria.getNomCategoria().equals(nombreNormalizado) && categoriaRepository.existsByNomCategoria(nombreNormalizado)) {
            throw new RuntimeException("El nombre de la categoría ya existe");
        }

        categoria.setNomCategoria(nombreNormalizado);
        categoria.setDescripcion(requestDTO.getDescripcion());
        categoria.setEstado(requestDTO.getEstado());

        CategoriaInstancia actualizada = categoriaRepository.save(categoria);
        return toResponseDTO(actualizada);
    }

    @Transactional
    public void darDeBajaCategoria(int id) {
        CategoriaInstancia categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría de instancia no encontrada"));
        
        categoria.setEstado(EstadoLogico.INACTIVO);
        categoriaRepository.save(categoria);
    }
}
