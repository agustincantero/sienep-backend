package com.fides.sienep.service;

import com.fides.sienep.dto.CarreraRequestDTO;
import com.fides.sienep.dto.CarreraResponseDTO;
import com.fides.sienep.mapper.CarreraMapper;
import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.CarreraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.List;

/**
 * Servicio encargado de la gestión completa de las Carreras.
 * 
 * <p><b>Mecanismo de Caché:</b></p>
 * <ul>
 *   <li>{@code @Cacheable("carreras")}: Guarda el resultado de la consulta en la memoria RAM del servidor.
 *       Las siguientes peticiones se responden de forma instantánea sin ir a PostgreSQL.</li>
 *   <li>{@code @CacheEvict(value = "carreras", allEntries = true)}: Actúa como la "destructora". 
 *       Cuando ocurre un alta, baja o modificación, esta anotación vacía la caja "carreras" de la memoria RAM.
 *       Esto obliga al sistema a volver a consultar la base de datos la próxima vez, garantizando 
 *       que nunca se le muestren datos viejos a los usuarios.</li>
 *   <li>Las escrituras vacían también la caja "itrs": {@code GET /itrs} incluye el nombre de las carreras
 *       de cada ITR, así que renombrar o desactivar una carrera deja desactualizado ese listado.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CarreraService {

    private final CarreraRepository carreraRepository;
    private final CarreraMapper carreraMapper;

    @PreAuthorize("hasAuthority('CREAR_CARRERA')")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "carreras", allEntries = true),
            @CacheEvict(value = "itrs", allEntries = true)
    })
    public CarreraResponseDTO crear(CarreraRequestDTO dto) {
        String nombre = dto.getNomCarrera().trim();
        if (carreraRepository.existsByNomCarreraIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una carrera con ese nombre.");
        }
        Carrera carrera = new Carrera();
        carrera.setNomCarrera(nombre);
        return carreraMapper.toDto(carreraRepository.save(carrera));
    }

    @PreAuthorize("hasAuthority('VER_CARRERAS')")
    @Transactional(readOnly = true)
    @Cacheable(value = "carreras", key = "#id")
    public CarreraResponseDTO obtenerPorId(Integer id) {
        return carreraMapper.toDto(buscarOFallar(id));
    }

    @PreAuthorize("hasAuthority('VER_CARRERAS')")
    @Transactional(readOnly = true)
    @Cacheable("carreras")
    public List<CarreraResponseDTO> listar(EstadoLogico estado) {
        EstadoLogico estadoEfectivo = estado != null ? estado : EstadoLogico.ACTIVO;
        return carreraMapper.toDtoList(carreraRepository.findByEstado(estadoEfectivo));
    }

    @PreAuthorize("hasAuthority('EDITAR_CARRERA')")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "carreras", allEntries = true),
            @CacheEvict(value = "itrs", allEntries = true)
    })
    public CarreraResponseDTO editar(Integer id, CarreraRequestDTO dto) {
        Carrera carrera = buscarOFallar(id);
        if (carrera.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede editar una carrera que no está ACTIVA.");
        }
        String nombre = dto.getNomCarrera().trim();
        // Si solo cambian mayúsculas/minúsculas del propio nombre no es un duplicado.
        if (!carrera.getNomCarrera().equalsIgnoreCase(nombre) && carreraRepository.existsByNomCarreraIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una carrera con ese nombre.");
        }
        carrera.setNomCarrera(nombre);
        return carreraMapper.toDto(carreraRepository.save(carrera));
    }

    @PreAuthorize("hasAuthority('DESACTIVAR_CARRERA')")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "carreras", allEntries = true),
            @CacheEvict(value = "itrs", allEntries = true)
    })
    public void desactivar(Integer id) {
        Carrera carrera = buscarOFallar(id);
        if (carrera.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La carrera ya se encuentra inactiva");
        }
        carrera.setEstado(EstadoLogico.INACTIVO);
        carreraRepository.save(carrera);
    }

    @PreAuthorize("hasAuthority('REACTIVAR_CARRERA')")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "carreras", allEntries = true),
            @CacheEvict(value = "itrs", allEntries = true)
    })
    public void reactivar(Integer id) {
        Carrera carrera = buscarOFallar(id);
        if (carrera.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La carrera ya se encuentra activa");
        }
        carrera.setEstado(EstadoLogico.ACTIVO);
        carreraRepository.save(carrera);
    }

    private Carrera buscarOFallar(Integer id) {
        return carreraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera no encontrada"));
    }
}