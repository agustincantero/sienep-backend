package com.fides.sienep.service;

import com.fides.sienep.dto.CategoriaRecordatorioCreateDTO;
import com.fides.sienep.dto.CategoriaRecordatorioResponseDTO;
import com.fides.sienep.dto.CategoriaRecordatorioUpdateDTO;
import com.fides.sienep.model.CategoriaRecordatorio;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.CategoriaRecordatorioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaRecordatorioService {

    private final CategoriaRecordatorioRepository categoriaRepository;

    private CategoriaRecordatorioResponseDTO toResponseDTO(CategoriaRecordatorio categoria) {
        CategoriaRecordatorioResponseDTO dto = new CategoriaRecordatorioResponseDTO();
        dto.setIdCategoria(categoria.getIdCategoria());
        dto.setNomCategoria(categoria.getNomCategoria());
        dto.setDescripcion(categoria.getDescripcion());
        dto.setEstado(categoria.getEstado());
        return dto;
    }

    @PreAuthorize("hasAuthority('VER_CATEGORIA_RECORDATORIO')")
    @Transactional(readOnly = true)
    public List<CategoriaRecordatorioResponseDTO> obtenerTodas(String nombre, EstadoLogico estado) {
        List<CategoriaRecordatorio> categorias = categoriaRepository.buscarConFiltros(nombre, estado);
        List<CategoriaRecordatorioResponseDTO> dtoList = new ArrayList<>();
        for (CategoriaRecordatorio cat : categorias) {
            dtoList.add(toResponseDTO(cat));
        }
        return dtoList;
    }

    @PreAuthorize("hasAuthority('VER_CATEGORIA_RECORDATORIO')")
    @Transactional(readOnly = true)
    public CategoriaRecordatorioResponseDTO obtenerPorId(int id) {
        CategoriaRecordatorio categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría de recordatorio no encontrada"));
        return toResponseDTO(categoria);
    }

    @PreAuthorize("hasAuthority('CREAR_CATEGORIA_RECORDATORIO')")
    @Transactional
    public CategoriaRecordatorioResponseDTO crearCategoria(CategoriaRecordatorioCreateDTO requestDTO) {
        String nombreNormalizado = requestDTO.getNomCategoria().trim().toUpperCase().replaceAll("\\s+", "_");

        if (categoriaRepository.existsByNomCategoria(nombreNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de la categoría ya existe");
        }

        CategoriaRecordatorio categoria = new CategoriaRecordatorio();
        categoria.setNomCategoria(nombreNormalizado);
        categoria.setDescripcion(requestDTO.getDescripcion());
        categoria.setEstado(EstadoLogico.ACTIVO); // Forzamos ACTIVO

        CategoriaRecordatorio guardada = categoriaRepository.save(categoria);
        return toResponseDTO(guardada);
    }

    @PreAuthorize("hasAuthority('EDITAR_CATEGORIA_RECORDATORIO')")
    @Transactional
    public CategoriaRecordatorioResponseDTO actualizarCategoria(int id, CategoriaRecordatorioUpdateDTO requestDTO) {
        CategoriaRecordatorio categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría de recordatorio no encontrada"));

        String nombreNormalizado = requestDTO.getNomCategoria().trim().toUpperCase().replaceAll("\\s+", "_");

        if (!categoria.getNomCategoria().equals(nombreNormalizado) && categoriaRepository.existsByNomCategoria(nombreNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de la categoría ya existe");
        }

        categoria.setNomCategoria(nombreNormalizado);
        categoria.setDescripcion(requestDTO.getDescripcion());
        categoria.setEstado(requestDTO.getEstado());

        CategoriaRecordatorio actualizada = categoriaRepository.save(categoria);
        return toResponseDTO(actualizada);
    }

    @PreAuthorize("hasAuthority('ELIMINAR_CATEGORIA_RECORDATORIO')")
    @Transactional
    public void darDeBajaCategoria(int id) {
        CategoriaRecordatorio categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría de recordatorio no encontrada"));
        
        categoria.setEstado(EstadoLogico.INACTIVO);
        categoriaRepository.save(categoria);
    }
}
