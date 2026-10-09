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
import com.fides.sienep.repository.IncidenciaSpecs;
import com.fides.sienep.utils.InstanciaIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    // Una incidencia registra algo que ya ocurrió: no puede quedar en el futuro.
    private void validarFechaNoFutura(LocalDateTime fechaHora) {
        if (fechaHora.isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "La fecha y hora de la incidencia no puede estar en el futuro");
        }
    }

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_INCIDENCIA')")
    public IncidenciaResponseDTO crear(IncidenciaRequestDTO dto) {
        validarFechaNoFutura(dto.getFechaHora());

        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un estudiante con el id " + dto.getIdEstudiante()));

        if (estudiante.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "El estudiante con id " + dto.getIdEstudiante() + " no está activo");
        }

        Funcionario funcionario = funcionarioRepository.findById(getIdUsuarioAutenticado())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el funcionario autenticado"));

        if (funcionario.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
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

    // Todos los filtros que lleguen se combinan (AND). Antes había un método por filtro (listarPorEstudiante,
    // listarPorFuncionario) y el controller usaba solo el primero presente: estudiante + funcionario devolvía
    // todas las incidencias del estudiante, sin filtrar por funcionario.
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INCIDENCIAS')")
    public Page<IncidenciaResponseDTO> listar(Integer idEstudiante, Integer idFuncionario, EstadoLogico estado,
                                              Pageable pageable) {
        EstadoLogico estadoEfectivo = estado != null ? estado : EstadoLogico.ACTIVO;

        Specification<Incidencia> spec = IncidenciaSpecs.estado(estadoEfectivo);
        if (idEstudiante != null) spec = spec.and(IncidenciaSpecs.estudiante(idEstudiante));
        if (idFuncionario != null) spec = spec.and(IncidenciaSpecs.funcionario(idFuncionario));

        Page<Incidencia> pagina = incidenciaRepository.findAll(spec, pageable);
        List<IncidenciaResponseDTO> dtos = incidenciaMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INCIDENCIAS')")
    public IncidenciaResponseDTO obtenerPorId(int id) {
        return incidenciaMapper.toDto(buscarActivaOFallar(id));
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public Page<IncidenciaResponseDTO> listarMisIncidencias(Pageable pageable) {
        Integer idEstudiante = getIdUsuarioAutenticado();
        Page<Incidencia> pagina = incidenciaRepository.findByEstudianteAndEstado(
                idEstudiante, EstadoLogico.ACTIVO, pageable);
        List<IncidenciaResponseDTO> dtos = incidenciaMapper.toDtoList(pagina.getContent());
        // El estudiante ve su propio documento, pero no el del funcionario responsable: es un dato del personal.
        dtos.forEach(dto -> dto.setDocumentoFuncionario(null));
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_INCIDENCIA')")
    public IncidenciaResponseDTO editar(int id, IncidenciaRequestDTO dto) {
        Incidencia incidencia = buscarActivaOFallar(id);

        // Solo si la fecha cambia, para no rechazar la edición de otros campos de una incidencia ya guardada.
        if (!dto.getFechaHora().truncatedTo(ChronoUnit.MINUTES).equals(incidencia.getFechaHora().truncatedTo(ChronoUnit.MINUTES))) {
            validarFechaNoFutura(dto.getFechaHora());
        }

        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un estudiante con el id " + dto.getIdEstudiante()));

        if (estudiante.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "El estudiante con id " + dto.getIdEstudiante() + " no está activo");
        }

        incidencia.setTitulo(dto.getTitulo());
        incidencia.setFechaHora(dto.getFechaHora());
        incidencia.setEstudiante(estudiante);
        incidencia.setCanal(dto.getCanal());
        incidencia.setLugar(dto.getLugar());

        log.info("Incidencia editada: {}", id);
        return incidenciaMapper.toDto(incidenciaRepository.save(incidencia));
    }

    @Transactional
    @PreAuthorize("hasAuthority('DESACTIVAR_INCIDENCIA')")
    public void desactivar(int id) {
        Incidencia incidencia = buscarActivaOFallar(id);
        incidencia.setEstado(EstadoLogico.INACTIVO);
        incidenciaRepository.save(incidencia);
        log.info("Baja lógica incidencia: {}", id);
    }

    @Transactional
    @PreAuthorize("hasAuthority('REACTIVAR_INCIDENCIA')")
    public IncidenciaResponseDTO reactivar(int id) {
        Incidencia incidencia = incidenciaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró una incidencia con el id " + id));

        if (incidencia.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La incidencia ya se encuentra activa");
        }

        incidencia.setEstado(EstadoLogico.ACTIVO);
        Incidencia guardada = incidenciaRepository.save(incidencia);
        log.info("Incidencia reactivada: {}", id);
        return incidenciaMapper.toDto(guardada);
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