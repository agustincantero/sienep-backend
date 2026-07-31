package com.fides.sienep.service;

import com.fides.sienep.dto.GrupoRequestDTO;
import com.fides.sienep.dto.GrupoResponseDTO;
import com.fides.sienep.dto.GrupoUpdateDTO;
import com.fides.sienep.mapper.GrupoMapper;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final CarreraRepository carreraRepository;
    private final GrupoMapper grupoMapper;

    @PreAuthorize("hasAuthority('CREAR_GRUPO')")
    @Transactional
    public GrupoResponseDTO crear(GrupoRequestDTO dto) {

        Carrera carrera = carreraRepository.findById(dto.getIdCarrera())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carrera no encontrada."));

        if (carrera.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede crear un grupo para una carrera inactiva.");
        }

        validarGeneracion(dto.getGeneracion());

        if (grupoRepository.existsByNomGrupoAndCarreraIdCarreraAndGeneracionAndEstado(
                dto.getNomGrupo(), dto.getIdCarrera(), dto.getGeneracion(), EstadoLogico.ACTIVO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un grupo activo con ese nombre para esa carrera y generación.");
        }

        Grupo grupo = Grupo.builder()
                .nomGrupo(dto.getNomGrupo())
                .carrera(carrera)
                .generacion(dto.getGeneracion())
                .build();

        return grupoMapper.toDto(grupoRepository.save(grupo));
    }

    @PreAuthorize("hasAuthority('VER_GRUPOS')")
    @Transactional(readOnly = true)
    public GrupoResponseDTO obtenerPorId(Integer id) {
        return grupoMapper.toDto(buscarOFallar(id));
    }

    @PreAuthorize("hasAuthority('VER_GRUPOS')")
    @Transactional(readOnly = true)
    public List<GrupoResponseDTO> listar(Integer idCarrera, Integer generacion) {

        List<Grupo> grupos;

        if (idCarrera != null && generacion != null) {
            grupos = grupoRepository.findByCarreraIdCarreraAndGeneracionAndEstado(idCarrera, generacion, EstadoLogico.ACTIVO);
        } else if (idCarrera != null) {
            grupos = grupoRepository.findByCarreraIdCarreraAndEstado(idCarrera, EstadoLogico.ACTIVO);
        } else if (generacion != null) {
            grupos = grupoRepository.findByGeneracionAndEstado(generacion, EstadoLogico.ACTIVO);
        } else {
            grupos = grupoRepository.findByEstado(EstadoLogico.ACTIVO);
        }

        return grupoMapper.toDtoList(grupos);
    }

    @PreAuthorize("hasAuthority('EDITAR_GRUPO')")
    @Transactional
    public GrupoResponseDTO actualizar(Integer id, GrupoUpdateDTO dto) {
        Grupo grupo = buscarOFallar(id);

        if (grupo.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede editar un grupo que no está ACTIVO.");
        }

        validarGeneracion(dto.getGeneracion());

        Optional<Grupo> grupoExistente = grupoRepository.findByNomGrupoAndCarreraIdCarreraAndGeneracionAndEstado(
                dto.getNomGrupo(), grupo.getCarrera().getIdCarrera(),
                dto.getGeneracion(), EstadoLogico.ACTIVO);
        if (grupoExistente.isPresent() && !grupoExistente.get().getIdGrupo().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe otro grupo activo con ese nombre para esa carrera y generación.");
        }

        grupo.setNomGrupo(dto.getNomGrupo());
        grupo.setGeneracion(dto.getGeneracion());
        return grupoMapper.toDto(grupoRepository.save(grupo));
    }

    @PreAuthorize("hasAuthority('DESACTIVAR_GRUPO')")
    @Transactional
    public void desactivar(Integer id) {
        Grupo grupo = buscarOFallar(id);
        if (grupo.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El grupo ya se encuentra inactivo");
        }
        grupo.setEstado(EstadoLogico.INACTIVO);
        grupoRepository.save(grupo);
    }

    @PreAuthorize("hasAuthority('REACTIVAR_GRUPO')")
    @Transactional
    public void reactivar(Integer id) {
        Grupo grupo = buscarOFallar(id);
        if (grupo.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El grupo ya se encuentra activo");
        }
        grupo.setEstado(EstadoLogico.ACTIVO);
        grupoRepository.save(grupo);
    }

    private void validarGeneracion(Integer generacion) {
        int anioActual = LocalDate.now().getYear();
        if (generacion <= 1900 || generacion > anioActual) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El año de generación debe ser mayor a 1900 y menor o igual al año actual (" + anioActual + ").");
        }
    }

    private Grupo buscarOFallar(Integer id) {
        return grupoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo no encontrado."));
    }
}