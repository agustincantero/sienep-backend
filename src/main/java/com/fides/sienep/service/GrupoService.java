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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final CarreraRepository carreraRepository;

    @Transactional
    public GrupoResponseDTO crear(GrupoRequestDTO dto) {

        Carrera carrera = carreraRepository.findById(dto.getIdCarrera())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Carrera no encontrada."
                ));

        if (carrera.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede crear un grupo para una carrera inactiva."
            );
        }

        if (grupoRepository.existsByNomGrupoAndCarreraIdCarreraAndGeneracionAndEstado(
                dto.getNomGrupo(), dto.getIdCarrera(), dto.getGeneracion(), EstadoLogico.ACTIVO)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un grupo activo con ese nombre para esa carrera y generación."
            );
        }

        Grupo grupo = Grupo.builder()
                .nomGrupo(dto.getNomGrupo())
                .carrera(carrera)
                .generacion(dto.getGeneracion())
                .build();

        return toResponseDTO(grupoRepository.save(grupo));
    }

    @Transactional(readOnly = true)
    public GrupoResponseDTO obtenerPorId(Integer id) {
        return toResponseDTO(buscarOFallar(id));
    }

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

        return grupos.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public GrupoResponseDTO actualizar(Integer id, GrupoRequestDTO dto) {

        Grupo grupo = buscarOFallar(id);

        // idCarrera no se modifica — cambiar la carrera de un grupo
        // es una operación de alto impacto que requeriría migrar estudiantes.
        grupo.setNomGrupo(dto.getNomGrupo());
        grupo.setGeneracion(dto.getGeneracion());

        return toResponseDTO(grupoRepository.save(grupo));
    }

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
                        HttpStatus.NOT_FOUND,
                        "Grupo no encontrado."
                ));
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