package com.fides.sienep.service;

import com.fides.sienep.dto.CategoriaInstanciaCreateDTO;
import com.fides.sienep.dto.CategoriaInstanciaResponseDTO;
import com.fides.sienep.dto.CategoriaInstanciaUpdateDTO;
import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.CategoriaInstanciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.fides.sienep.mapper.CategoriaInstanciaMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaInstanciaService {

    private final CategoriaInstanciaRepository categoriaRepository;
    private final CategoriaInstanciaMapper mapper;

    // --- Métodos CRUD ---
    @PreAuthorize("hasAuthority('VER_CATEGORIA_INSTANCIA')")
    @Transactional(readOnly = true)
    @Cacheable("categoriasInstancia")
    public List<CategoriaInstanciaResponseDTO> obtenerTodas(String nombre, EstadoLogico estado) {
        List<CategoriaInstancia> categorias = categoriaRepository.buscarConFiltros(nombre, estado);
        return mapper.toDtoList(categorias);
    }

    @PreAuthorize("hasAuthority('VER_CATEGORIA_INSTANCIA')")
    @Transactional(readOnly = true)
    @Cacheable(value = "categoriasInstancia", key = "#id")
    public CategoriaInstanciaResponseDTO obtenerPorId(int id) {
        CategoriaInstancia categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría de instancia no encontrada"));
        return mapper.toDto(categoria);
    }

    @PreAuthorize("hasAuthority('CREAR_CATEGORIA_INSTANCIA')")
    @Transactional
    @CacheEvict(value = "categoriasInstancia", allEntries = true)
    public CategoriaInstanciaResponseDTO crearCategoria(CategoriaInstanciaCreateDTO requestDTO) {
        // Normalización del nombre
        String nombreNormalizado = requestDTO.getNomCategoria().trim().toUpperCase().replaceAll("\\s+", "_");

        if (categoriaRepository.existsByNomCategoria(nombreNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de la categoría ya existe");
        }

        CategoriaInstancia categoria = new CategoriaInstancia();
        categoria.setNomCategoria(nombreNormalizado);
        categoria.setDescripcion(requestDTO.getDescripcion());
        categoria.setEstado(EstadoLogico.ACTIVO); // Forzamos ACTIVO al crear

        CategoriaInstancia guardada = categoriaRepository.save(categoria);
        return mapper.toDto(guardada);
    }

    @PreAuthorize("hasAuthority('EDITAR_CATEGORIA_INSTANCIA')")
    @Transactional
    @CacheEvict(value = "categoriasInstancia", allEntries = true)
    public CategoriaInstanciaResponseDTO actualizarCategoria(int id, CategoriaInstanciaUpdateDTO requestDTO) {
        CategoriaInstancia categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría de instancia no encontrada"));

        if (categoria.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede editar una categoría inactiva. Por favor, reactívela primero.");
        }

        // Normalización del nombre
        String nombreNormalizado = requestDTO.getNomCategoria().trim().toUpperCase().replaceAll("\\s+", "_");

        // Validar nombre único si lo están cambiando
        if (!categoria.getNomCategoria().equals(nombreNormalizado) && categoriaRepository.existsByNomCategoria(nombreNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de la categoría ya existe");
        }

        categoria.setNomCategoria(nombreNormalizado);
        categoria.setDescripcion(requestDTO.getDescripcion());

        CategoriaInstancia actualizada = categoriaRepository.save(categoria);
        return mapper.toDto(actualizada);
    }

    @PreAuthorize("hasAuthority('ELIMINAR_CATEGORIA_INSTANCIA')")
    @Transactional
    @CacheEvict(value = "categoriasInstancia", allEntries = true)
    public void darDeBajaCategoria(int id) {
        CategoriaInstancia categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría de instancia no encontrada"));
        
        if (categoria.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La categoría ya se encuentra inactiva");
        }
        
        categoria.setEstado(EstadoLogico.INACTIVO);
        categoriaRepository.save(categoria);
    }

    @PreAuthorize("hasAuthority('REACTIVAR_CATEGORIA_INSTANCIA')")
    @Transactional
    @CacheEvict(value = "categoriasInstancia", allEntries = true)
    public CategoriaInstanciaResponseDTO reactivarCategoria(int id) {
        CategoriaInstancia categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría de instancia no encontrada"));

        if (categoria.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La categoría ya se encuentra activa");
        }

        categoria.setEstado(EstadoLogico.ACTIVO);
        return mapper.toDto(categoriaRepository.save(categoria));
    }
}
