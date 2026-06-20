package com.fides.sienep.service;

import com.fides.sienep.dto.IncidenciaRequestDTO;
import com.fides.sienep.dto.IncidenciaResponseDTO;
import com.fides.sienep.mapper.IncidenciaMapper;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Incidencia;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.IncidenciaRepository;
import com.fides.sienep.utils.InstanciaIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final EstudianteRepository estudianteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final InstanciaIdGenerator instanciaIdGenerator;
    private final IncidenciaMapper incidenciaMapper;

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(auth.getName());
    }

    @Transactional
    @PreAuthorize("hasAuthority('REGISTRAR_INCIDENCIA')")
    public IncidenciaResponseDTO crear(IncidenciaRequestDTO dto) {
        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un estudiante con el id " + dto.getIdEstudiante()));

        if (estudiante.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "El estudiante con id " + dto.getIdEstudiante() + " no está activo");
        }

        Funcionario funcionario = funcionarioRepository.findById(getIdUsuarioAutenticado())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el funcionario autenticado"));

        if (funcionario.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "El funcionario autenticado no está activo");
        }

        Incidencia incidencia = new Incidencia(
                dto.getTitulo(),
                dto.getFechaHora(),
                estudiante,
                funcionario,
                dto.getCanal(),
                dto.getLugar()
        );

        String idNeg = instanciaIdGenerator.generarIdNeg();
        incidencia.setIdNegInstancia(idNeg);

        Incidencia guardada = incidenciaRepository.save(incidencia);
        log.info("Incidencia creada: {} con ID negocio: {}", guardada.getCodInstancia(), idNeg);
        return incidenciaMapper.toDto(guardada);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INCIDENCIAS')")
    public List<IncidenciaResponseDTO> listarActivas() {
        return incidenciaMapper.toDtoList(
                incidenciaRepository.findByEstado(EstadoLogico.ACTIVO));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INCIDENCIAS')")
    public IncidenciaResponseDTO buscarPorId(int id) {
        return incidenciaMapper.toDto(buscarActivaOFallar(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INCIDENCIAS')")
    public List<IncidenciaResponseDTO> listarPorEstudiante(int idEstudiante) {
        return incidenciaMapper.toDtoList(
                incidenciaRepository.findByEstudianteAndEstado(idEstudiante, EstadoLogico.ACTIVO));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INCIDENCIAS')")
    public List<IncidenciaResponseDTO> listarPorFuncionario(int idFuncionario) {
        return incidenciaMapper.toDtoList(
                incidenciaRepository.findByFuncionarioAndEstado(idFuncionario, EstadoLogico.ACTIVO));
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public List<IncidenciaResponseDTO> listarMisIncidencias() {
        Integer idEstudiante = getIdUsuarioAutenticado();
        return incidenciaMapper.toDtoList(
                incidenciaRepository.findByEstudianteAndEstado(idEstudiante, EstadoLogico.ACTIVO));
    }

    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_INCIDENCIAS')")
    public IncidenciaResponseDTO actualizar(int id, IncidenciaRequestDTO dto) {
        Incidencia incidencia = buscarActivaOFallar(id);

        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un estudiante con el id " + dto.getIdEstudiante()));

        incidencia.setTitulo(dto.getTitulo());
        incidencia.setFechaHora(dto.getFechaHora());
        incidencia.setEstudiante(estudiante);
        incidencia.setCanal(dto.getCanal());
        incidencia.setLugar(dto.getLugar());

        log.info("Incidencia actualizada: {}", id);
        return incidenciaMapper.toDto(incidenciaRepository.save(incidencia));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ELIMINAR_INCIDENCIAS')")
    public void darDeBaja(int id) {
        Incidencia incidencia = buscarActivaOFallar(id);
        incidencia.setEstado(EstadoLogico.INACTIVO);
        incidenciaRepository.save(incidencia);
        log.info("Baja lógica incidencia: {}", id);
    }

    // ----------------------------------------------------------------
    // Helpers privados
    // ----------------------------------------------------------------

    private Incidencia buscarActivaOFallar(int id) {
        return incidenciaRepository.findByCodInstanciaAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una incidencia activa con el id " + id));
    }
}