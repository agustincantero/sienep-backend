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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import com.fides.sienep.mapper.CategoriaRecordatorioMapper;

import java.util.List;

/**
 * Servicio encargado de la gestión completa de las Categorías de Recordatorios.
 * 
 * <p><b>Mecanismo de Caché:</b></p>
 * <ul>
 *   <li>{@code @Cacheable("categoriasRecordatorio")}: Guarda el resultado de la consulta en la memoria RAM del servidor.
 *       Las siguientes peticiones se responden de forma instantánea sin ir a PostgreSQL.</li>
 *   <li>{@code @CacheEvict(value = "categoriasRecordatorio", allEntries = true)}: Actúa como la "destructora". 
 *       Cuando ocurre un alta, baja o modificación, esta anotación vacía la caja "categoriasRecordatorio" de la memoria RAM.
 *       Esto obliga al sistema a volver a consultar la base de datos la próxima vez, garantizando 
 *       que nunca se le muestren datos viejos a los usuarios.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CategoriaRecordatorioService {

    private final CategoriaRecordatorioRepository categoriaRepository;
    private final CategoriaRecordatorioMapper mapper;

    @PreAuthorize("hasAuthority('VER_CATEGORIAS_RECORDATORIO')")
    @Transactional(readOnly = true)
    @Cacheable("categoriasRecordatorio")
    public List<CategoriaRecordatorioResponseDTO> obtenerTodas(String nombre, EstadoLogico estado) {
        List<CategoriaRecordatorio> categorias = categoriaRepository.buscarConFiltros(nombre, estado);
        return mapper.toDtoList(categorias);
    }

    @PreAuthorize("hasAuthority('VER_CATEGORIAS_RECORDATORIO')")
    @Transactional(readOnly = true)
    @Cacheable(value = "categoriasRecordatorio", key = "#id")
    public CategoriaRecordatorioResponseDTO obtenerPorId(int id) {
        CategoriaRecordatorio categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría de recordatorio no encontrada"));
        return mapper.toDto(categoria);
    }

    @PreAuthorize("hasAuthority('CREAR_CATEGORIA_RECORDATORIO')")
    @Transactional
    @CacheEvict(value = "categoriasRecordatorio", allEntries = true)
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
        return mapper.toDto(guardada);
    }

    @PreAuthorize("hasAuthority('EDITAR_CATEGORIA_RECORDATORIO')")
    @Transactional
    @CacheEvict(value = "categoriasRecordatorio", allEntries = true)
    public CategoriaRecordatorioResponseDTO actualizarCategoria(int id, CategoriaRecordatorioUpdateDTO requestDTO) {
        CategoriaRecordatorio categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría de recordatorio no encontrada"));

        if (categoria.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede editar una categoría inactiva. Por favor, reactívela primero.");
        }

        String nombreNormalizado = requestDTO.getNomCategoria().trim().toUpperCase().replaceAll("\\s+", "_");

        if (!categoria.getNomCategoria().equals(nombreNormalizado) && categoriaRepository.existsByNomCategoria(nombreNormalizado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de la categoría ya existe");
        }

        categoria.setNomCategoria(nombreNormalizado);
        categoria.setDescripcion(requestDTO.getDescripcion());

        CategoriaRecordatorio actualizada = categoriaRepository.save(categoria);
        return mapper.toDto(actualizada);
    }

    @PreAuthorize("hasAuthority('DESACTIVAR_CATEGORIA_RECORDATORIO')")
    @Transactional
    @CacheEvict(value = "categoriasRecordatorio", allEntries = true)
    public void desactivarCategoria(int id) {
        CategoriaRecordatorio categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría de recordatorio no encontrada"));

        if (categoria.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La categoría ya se encuentra inactiva");
        }

        if (categoriaRepository.existeRecordatorioActivoConCategoria(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede desactivar la categoría porque tiene recordatorios activos asociados");
        }

        categoria.setEstado(EstadoLogico.INACTIVO);
        categoriaRepository.save(categoria);
    }

    @PreAuthorize("hasAuthority('REACTIVAR_CATEGORIA_RECORDATORIO')")
    @Transactional
    @CacheEvict(value = "categoriasRecordatorio", allEntries = true)
    public CategoriaRecordatorioResponseDTO reactivarCategoria(int id) {
        CategoriaRecordatorio categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría de recordatorio no encontrada"));

        if (categoria.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La categoría ya se encuentra activa");
        }

        categoria.setEstado(EstadoLogico.ACTIVO);
        return mapper.toDto(categoriaRepository.save(categoria));
    }
}
