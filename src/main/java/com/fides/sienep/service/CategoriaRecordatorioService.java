package com.fides.sienep.service;

import com.fides.sienep.dto.CategoriaRecordatorioCreateDTO;
import com.fides.sienep.dto.CategoriaRecordatorioResponseDTO;
import com.fides.sienep.dto.CategoriaRecordatorioUpdateDTO;
import com.fides.sienep.model.CategoriaRecordatorio;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.CategoriaRecordatorioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoriaRecordatorioService {

    private final CategoriaRecordatorioRepository categoriaRepository;

    @Autowired
    public CategoriaRecordatorioService(CategoriaRecordatorioRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    private CategoriaRecordatorioResponseDTO toResponseDTO(CategoriaRecordatorio categoria) {
        CategoriaRecordatorioResponseDTO dto = new CategoriaRecordatorioResponseDTO();
        dto.setIdCategoria(categoria.getIdCategoria());
        dto.setNomCategoria(categoria.getNomCategoria());
        dto.setDescripcion(categoria.getDescripcion());
        dto.setEstado(categoria.getEstado());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<CategoriaRecordatorioResponseDTO> obtenerTodas(String nombre, EstadoLogico estado) {
        List<CategoriaRecordatorio> categorias = categoriaRepository.buscarConFiltros(nombre, estado);
        List<CategoriaRecordatorioResponseDTO> dtoList = new ArrayList<>();
        for (CategoriaRecordatorio cat : categorias) {
            dtoList.add(toResponseDTO(cat));
        }
        return dtoList;
    }

    @Transactional(readOnly = true)
    public CategoriaRecordatorioResponseDTO obtenerPorId(int id) {
        CategoriaRecordatorio categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría de recordatorio no encontrada"));
        return toResponseDTO(categoria);
    }

    @Transactional
    public CategoriaRecordatorioResponseDTO crearCategoria(CategoriaRecordatorioCreateDTO requestDTO) {
        String nombreNormalizado = requestDTO.getNomCategoria().trim().toUpperCase().replaceAll("\\s+", "_");

        if (categoriaRepository.existsByNomCategoria(nombreNormalizado)) {
            throw new RuntimeException("El nombre de la categoría ya existe");
        }

        CategoriaRecordatorio categoria = new CategoriaRecordatorio();
        categoria.setNomCategoria(nombreNormalizado);
        categoria.setDescripcion(requestDTO.getDescripcion());
        categoria.setEstado(EstadoLogico.ACTIVO); // Forzamos ACTIVO

        CategoriaRecordatorio guardada = categoriaRepository.save(categoria);
        return toResponseDTO(guardada);
    }

    @Transactional
    public CategoriaRecordatorioResponseDTO actualizarCategoria(int id, CategoriaRecordatorioUpdateDTO requestDTO) {
        CategoriaRecordatorio categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría de recordatorio no encontrada"));

        String nombreNormalizado = requestDTO.getNomCategoria().trim().toUpperCase().replaceAll("\\s+", "_");

        if (!categoria.getNomCategoria().equals(nombreNormalizado) && categoriaRepository.existsByNomCategoria(nombreNormalizado)) {
            throw new RuntimeException("El nombre de la categoría ya existe");
        }

        categoria.setNomCategoria(nombreNormalizado);
        categoria.setDescripcion(requestDTO.getDescripcion());
        categoria.setEstado(requestDTO.getEstado());

        CategoriaRecordatorio actualizada = categoriaRepository.save(categoria);
        return toResponseDTO(actualizada);
    }

    @Transactional
    public void darDeBajaCategoria(int id) {
        CategoriaRecordatorio categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría de recordatorio no encontrada"));
        
        categoria.setEstado(EstadoLogico.INACTIVO);
        categoriaRepository.save(categoria);
    }
}
