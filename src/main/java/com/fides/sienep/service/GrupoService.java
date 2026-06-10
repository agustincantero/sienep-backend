package com.fides.sienep.service;

import com.fides.sienep.dto.GrupoRequestDTO;
import com.fides.sienep.dto.GrupoResponseDTO;
import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Grupo;
import com.fides.sienep.repository.CarreraRepository;
import com.fides.sienep.repository.GrupoRepository;
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
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final CarreraRepository carreraRepository;

    // ── Decisión de permisos para Grupo: ─────────────────────────────────────
    //
    // El schema SQL no define permisos granulares de CRUD para Grupo.
    // Grupo es una entidad de configuración académica (como Carrera o ITR)
    // → solo el ADMINISTRADOR puede crearlos, editarlos o darlos de baja.
    //
    // Para lecturas: isAuthenticated() — cualquier usuario con sesión
    // válida puede consultar grupos (necesitan verlos para asignar estudiantes).
    //
    // Alternativa que NO se usa: hasAuthority('CREAR_GRUPO') etc.
    // No existe ese permiso en la BD → nunca pasaría la evaluación.
    // Inventar @PreAuthorize con strings que no existen en la BD es
    // un error grave: silencioso en compilación, bloquea en runtime.

    @PreAuthorize("hasAuthority('CREAR_GRUPO')")
    @Transactional
    public GrupoResponseDTO crear(GrupoRequestDTO dto) {

        Carrera carrera = carreraRepository.findById(dto.getIdCarrera())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Carrera no encontrada."));

        if (carrera.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede crear un grupo para una carrera inactiva.");
        }

        if (grupoRepository.existsByNomGrupoAndCarreraIdCarreraAndGeneracionAndEstado(
                dto.getNomGrupo(), dto.getIdCarrera(), dto.getGeneracion(), EstadoLogico.ACTIVO)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un grupo activo con ese nombre para esa carrera y generación.");
        }

        Grupo grupo = Grupo.builder()
                .nomGrupo(dto.getNomGrupo())
                .carrera(carrera)
                .generacion(dto.getGeneracion())
                .build();

        return toResponseDTO(grupoRepository.save(grupo));
    }

    // ── isAuthenticated() vs hasRole() para lecturas ──────────────────────────
    // isAuthenticated() = "tiene token JWT válido, sin importar el rol".
    // Todos los roles del sistema necesitan ver grupos para trabajar.
    // Poner hasRole('ADMINISTRADOR') en lecturas sería demasiado restrictivo.
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public GrupoResponseDTO obtenerPorId(Integer id) {
        return toResponseDTO(buscarOFallar(id));
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<GrupoResponseDTO> listar(Integer idCarrera, Integer generacion) {

        List<Grupo> grupos;

        if (idCarrera != null && generacion != null) {
            grupos = grupoRepository.findByCarreraIdCarreraAndGeneracionAndEstado(
                    idCarrera, generacion, EstadoLogico.ACTIVO);
        } else if (idCarrera != null) {
            grupos = grupoRepository.findByCarreraIdCarreraAndEstado(
                    idCarrera, EstadoLogico.ACTIVO);
        } else if (generacion != null) {
            grupos = grupoRepository.findByGeneracionAndEstado(
                    generacion, EstadoLogico.ACTIVO);
        } else {
            grupos = grupoRepository.findByEstado(EstadoLogico.ACTIVO);
        }

        List<GrupoResponseDTO> dtos = new ArrayList<>();
        for (Grupo grupo : grupos) {
            dtos.add(toResponseDTO(grupo));
        }
        return dtos;
    }

    @PreAuthorize("hasAuthority('EDITAR_GRUPOS')")
    @Transactional
    public GrupoResponseDTO actualizar(Integer id, GrupoRequestDTO dto) {
        Grupo grupo = buscarOFallar(id);

        if (grupoRepository.existsByNomGrupoAndCarreraIdCarreraAndGeneracionAndEstado(
                dto.getNomGrupo(), grupo.getCarrera().getIdCarrera(),
                dto.getGeneracion(), EstadoLogico.ACTIVO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un grupo activo con ese nombre para esa carrera y generación.");
        }

        grupo.setNomGrupo(dto.getNomGrupo());
        grupo.setGeneracion(dto.getGeneracion());
        return toResponseDTO(grupoRepository.save(grupo));
    }

    @PreAuthorize("hasAuthority('ELIMINAR_GRUPOS')")
    @Transactional
    public void darDeBaja(Integer id) {
        Grupo grupo = buscarOFallar(id);
        grupo.setEstado(EstadoLogico.INACTIVO);
        grupoRepository.save(grupo);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Grupo buscarOFallar(Integer id) {
        return grupoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Grupo no encontrado."));
    }

    private GrupoResponseDTO toResponseDTO(Grupo grupo) {
        return GrupoResponseDTO.builder()
                .idGrupo(grupo.getIdGrupo())
                .nomGrupo(grupo.getNomGrupo())
                .generacion(grupo.getGeneracion())
                .estado(grupo.getEstado())
                .idCarrera(grupo.getCarrera().getIdCarrera())
                .nomCarrera(grupo.getCarrera().getNomCarrera())
                .createdAt(grupo.getCreatedAt())
                .updatedAt(grupo.getUpdatedAt())
                .build();
    }
}