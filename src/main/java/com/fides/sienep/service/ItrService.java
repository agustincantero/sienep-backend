package com.fides.sienep.service;

import com.fides.sienep.dto.ItrRequestDTO;
import com.fides.sienep.dto.ItrResponseDTO;
import com.fides.sienep.mapper.ItrMapper;
import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Itr;
import com.fides.sienep.model.ItrCarrera;
import com.fides.sienep.repository.CarreraRepository;
import com.fides.sienep.repository.ItrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

/**
 * Servicio encargado de la gestión completa de los ITRs y su asociación con Carreras (Aggregate Root).
 * 
 * <p><b>Mecanismo de Caché:</b></p>
 * <ul>
 *   <li>{@code @Cacheable("itrs")}: Guarda el resultado de la consulta en la memoria RAM del servidor.
 *       Las siguientes peticiones se responden de forma instantánea sin ir a PostgreSQL.</li>
 *   <li>{@code @CacheEvict(value = "itrs", allEntries = true)}: Actúa como la "destructora". 
 *       Cuando ocurre un alta, baja o modificación, esta anotación vacía la caja "itrs" de la memoria RAM.
 *       Esto obliga al sistema a volver a consultar la base de datos la próxima vez, garantizando 
 *       que nunca se le muestren datos viejos a los usuarios.</li>
 *   <li>Las escrituras vacían también la caja "carreras": {@code GET /carreras} incluye los ITRs de cada
 *       carrera, así que asociar o desvincular carreras, o renombrar/desactivar un ITR, deja desactualizado ese listado.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ItrService {

    private final ItrRepository itrRepository;
    private final CarreraRepository carreraRepository;
    private final ItrMapper itrMapper;

    /**
     * Crea un nuevo ITR y purga la caché.
     * Al usar @CacheEvict con allEntries = true, vaciamos la colección "itrs" 
     * en memoria para garantizar que el próximo GET traiga este nuevo registro.
     */
    @PreAuthorize("hasAuthority('CREAR_ITR')")
    @Caching(evict = {
            @CacheEvict(value = "itrs", allEntries = true),
            @CacheEvict(value = "carreras", allEntries = true)
    })
    @Transactional
    public ItrResponseDTO crear(ItrRequestDTO dto) {
        String nombre = dto.getNomItr().trim();
        if (itrRepository.existsByNomItrIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un ITR con ese nombre.");
        }

        Itr itr = new Itr(nombre);

        // Si no hay carreras a asociar, se guarda una sola vez y listo.
        if (dto.getCarrerasIds() == null || dto.getCarrerasIds().isEmpty()) {
            return itrMapper.toDto(itrRepository.save(itr));
        }

        // Si hay carreras, guardamos primero para generar el ID de ITR en BD
        itr = itrRepository.save(itr); 

        asociarCarreras(itr, dto.getCarrerasIds());

        // Guardamos por segunda vez con las relaciones
        return itrMapper.toDto(itrRepository.save(itr)); 
    }

    /**
     * Lista los ITRs por estado (ACTIVO por defecto si no se especifica), utilizando
     * caché en memoria. Gracias a @Cacheable("itrs"), la primera consulta va a la BD,
     * pero las siguientes se leen instantáneamente desde la RAM hasta que ocurra un @CacheEvict.
     */
    @PreAuthorize("hasAuthority('VER_ITRS')")
    @Cacheable("itrs")
    @Transactional(readOnly = true)
    public List<ItrResponseDTO> listar(EstadoLogico estado) {
        EstadoLogico estadoEfectivo = estado != null ? estado : EstadoLogico.ACTIVO;
        return itrMapper.toDtoList(itrRepository.findByEstado(estadoEfectivo));
    }

    @PreAuthorize("hasAuthority('VER_ITRS')")
    @Transactional(readOnly = true)
    public ItrResponseDTO obtenerPorId(Integer id) {
        return itrMapper.toDto(buscarOFallar(id));
    }

    /**
     * Edita un ITR y purga la caché para evitar que los usuarios 
     * sigan viendo datos viejos en el listado.
     */
    @PreAuthorize("hasAuthority('EDITAR_ITR')")
    @Caching(evict = {
            @CacheEvict(value = "itrs", allEntries = true),
            @CacheEvict(value = "carreras", allEntries = true)
    })
    @Transactional
    public ItrResponseDTO editar(Integer id, ItrRequestDTO dto) {
        Itr itr = buscarOFallar(id);

        if (itr.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede modificar ni asociar carreras a un ITR inactivo.");
        }

        String nombre = dto.getNomItr().trim();
        if (!itr.getNomItr().equalsIgnoreCase(nombre) && itrRepository.existsByNomItrIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre del ITR ya está en uso.");
        }

        itr.setNomItr(nombre);
        asociarCarreras(itr, dto.getCarrerasIds());

        return itrMapper.toDto(itrRepository.save(itr));
    }

    /**
     * Da de baja lógica un ITR. Evict de caché requerido para actualizar catálogo.
     */
    @PreAuthorize("hasAuthority('DESACTIVAR_ITR')")
    @Caching(evict = {
            @CacheEvict(value = "itrs", allEntries = true),
            @CacheEvict(value = "carreras", allEntries = true)
    })
    @Transactional
    public void desactivar(Integer id) {
        Itr itr = buscarOFallar(id);
        if (itr.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El ITR ya se encuentra inactivo");
        }
        itr.setEstado(EstadoLogico.INACTIVO);
        itrRepository.save(itr);
    }

    /**
     * Reactiva un ITR inactivo. Evict de caché requerido para actualizar catálogo.
     */
    @PreAuthorize("hasAuthority('REACTIVAR_ITR')")
    @Caching(evict = {
            @CacheEvict(value = "itrs", allEntries = true),
            @CacheEvict(value = "carreras", allEntries = true)
    })
    @Transactional
    public void reactivar(Integer id) {
        Itr itr = buscarOFallar(id);
        if (itr.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El ITR ya se encuentra activo");
        }
        itr.setEstado(EstadoLogico.ACTIVO);
        itrRepository.save(itr);
    }

    private void asociarCarreras(Itr itr, List<Integer> nuevasCarrerasIds) {
        if (nuevasCarrerasIds == null) {
            nuevasCarrerasIds = Collections.emptyList();
        }

        for (ItrCarrera itrCarrera : itr.getCarreras()) {
            if (itrCarrera.getEstado() == EstadoLogico.ACTIVO && 
                !nuevasCarrerasIds.contains(itrCarrera.getCarrera().getIdCarrera())) {
                itrCarrera.setEstado(EstadoLogico.INACTIVO);
            }
        }

        for (Integer idCarrera : nuevasCarrerasIds) {
            boolean existeAsociacion = false;

            for (ItrCarrera itrCarrera : itr.getCarreras()) {
                if (itrCarrera.getCarrera().getIdCarrera().equals(idCarrera)) {
                    if (itrCarrera.getEstado() == EstadoLogico.INACTIVO) {
                        // Reactivar el vínculo no debería saltearse la validación
                        // de que la carrera referenciada siga ACTIVA.
                        if (itrCarrera.getCarrera().getEstado() != EstadoLogico.ACTIVO) {
                            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                                    "No se puede asociar una carrera que no está ACTIVA: " + idCarrera);
                        }
                        itrCarrera.setEstado(EstadoLogico.ACTIVO);
                    }
                    existeAsociacion = true;
                    break;
                }
            }

            if (!existeAsociacion) {
                Carrera carrera = carreraRepository.findById(idCarrera)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera con ID " + idCarrera + " no encontrada."));
                if (carrera.getEstado() != EstadoLogico.ACTIVO) {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                            "No se puede asociar una carrera que no está ACTIVA: " + idCarrera);
                }
                ItrCarrera nuevaAsociacion = new ItrCarrera(itr, carrera);
                nuevaAsociacion.setEstado(EstadoLogico.ACTIVO);
                itr.getCarreras().add(nuevaAsociacion);
            }
        }
    }

    private Itr buscarOFallar(Integer id) {
        return itrRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ITR no encontrado: " + id));
    }
}