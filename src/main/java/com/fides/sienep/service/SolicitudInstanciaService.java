package com.fides.sienep.service;

import com.fides.sienep.dto.AtenderSolicitudInstanciaDTO;
import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.dto.RechazarSolicitudInstanciaDTO;
import com.fides.sienep.dto.SolicitudInstanciaRequestDTO;
import com.fides.sienep.dto.SolicitudInstanciaResponseDTO;
import com.fides.sienep.mapper.SolicitudInstanciaMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.EstadoSolicitud;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.SolicitudInstancia;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.repository.SolicitudInstanciaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

// Solicitudes de instancia: el estudiante pide una instancia desde "Mi perfil" y un funcionario la atiende
// (creando la instancia) o la rechaza con una respuesta.
@Service
@RequiredArgsConstructor
public class SolicitudInstanciaService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudInstanciaService.class);

    // Tope de solicitudes PENDIENTES por estudiante, para que no se acumulen pedidos repetidos sin atender.
    static final int MAX_PENDIENTES_POR_ESTUDIANTE = 3;

    private static final String TITULO_POR_DEFECTO = "Solicitud del estudiante";

    private final SolicitudInstanciaRepository solicitudRepository;
    private final EstudianteRepository estudianteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final InstanciaComunRepository instanciaComunRepository;
    private final InstanciaComunService instanciaComunService;
    private final SolicitudInstanciaMapper solicitudMapper;

    // =========================================================
    // ESTUDIANTE
    // =========================================================

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional
    public SolicitudInstanciaResponseDTO crear(SolicitudInstanciaRequestDTO dto) {
        int idEstudiante = getIdUsuarioAutenticado();
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));

        // Mismo criterio que InstanciaComunService.crear(): una instancia solo se le crea a un estudiante
        // ACTIVO, así que no tiene sentido aceptar una solicitud que después no se podría atender.
        if (estudiante.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Tu cuenta no está activa, no podés solicitar instancias.");
        }

        if (solicitudRepository.countByEstudianteIdUsuarioAndEstado(idEstudiante, EstadoSolicitud.PENDIENTE)
                >= MAX_PENDIENTES_POR_ESTUDIANTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya tenés " + MAX_PENDIENTES_POR_ESTUDIANTE + " solicitudes pendientes. " +
                            "Esperá a que se respondan antes de enviar otra.");
        }

        SolicitudInstancia solicitud = solicitudRepository.save(new SolicitudInstancia(estudiante, dto.getMotivo().trim()));
        log.info("[AUDIT] Estudiante id={} creó la solicitud de instancia {}", idEstudiante, solicitud.getIdSolicitud());
        return solicitudMapper.toDto(solicitud);
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public Page<SolicitudInstanciaResponseDTO> listarMias(Pageable pageable) {
        return solicitudRepository.findByEstudianteIdUsuario(getIdUsuarioAutenticado(), pageable)
                .map(solicitudMapper::toDto);
    }

    // =========================================================
    // FUNCIONARIO
    // =========================================================

    // Sin estado: todas. Con estado (ej. PENDIENTE): la bandeja de trabajo.
    @PreAuthorize("hasAuthority('VER_SOLICITUDES_INSTANCIA')")
    @Transactional(readOnly = true)
    public Page<SolicitudInstanciaResponseDTO> listar(EstadoSolicitud estado, Pageable pageable) {
        Page<SolicitudInstancia> pagina = estado != null
                ? solicitudRepository.findByEstado(estado, pageable)
                : solicitudRepository.findAll(pageable);
        return pagina.map(solicitudMapper::toDto);
    }

    // El estudiante puede ver una propia; quién es el dueño se conoce recién al cargarla.
    @PreAuthorize("hasAuthority('VER_SOLICITUDES_INSTANCIA') or hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public SolicitudInstanciaResponseDTO obtenerPorId(int idSolicitud) {
        SolicitudInstancia solicitud = buscarOFallar(idSolicitud);
        if (!tienePermiso("VER_SOLICITUDES_INSTANCIA")
                && solicitud.getEstudiante().getIdUsuario() != getIdUsuarioAutenticado()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes ver tus propias solicitudes.");
        }
        return solicitudMapper.toDto(solicitud);
    }

    // Atender = crear la instancia para el estudiante de la solicitud y dejarla enlazada. Exige también
    // CREAR_INSTANCIA porque eso es lo que hace; la creación pasa por InstanciaComunService.crear() (por el
    // proxy de Spring, así que corre sus mismas validaciones, notificación y sync con Calendar) dentro de
    // esta misma transacción: si crear() falla, la solicitud queda PENDIENTE.
    @PreAuthorize("hasAuthority('ATENDER_SOLICITUD_INSTANCIA') and hasAuthority('CREAR_INSTANCIA')")
    @Transactional
    public SolicitudInstanciaResponseDTO atender(int idSolicitud, AtenderSolicitudInstanciaDTO dto) {
        SolicitudInstancia solicitud = buscarPendienteParaResolver(idSolicitud);

        InstanciaComunRequestDTO instanciaDto = new InstanciaComunRequestDTO();
        instanciaDto.setTitulo(dto.getTitulo() != null && !dto.getTitulo().isBlank()
                ? dto.getTitulo().trim()
                : TITULO_POR_DEFECTO);
        instanciaDto.setFechaHora(dto.getFechaHora());
        instanciaDto.setIdEstudiante(solicitud.getEstudiante().getIdUsuario());
        instanciaDto.setCanal(dto.getCanal());
        instanciaDto.setIdCategoria(dto.getIdCategoria());
        InstanciaComunResponseDTO instancia = instanciaComunService.crear(instanciaDto);

        resolver(solicitud, EstadoSolicitud.ATENDIDA, dto.getRespuesta());
        solicitud.setInstancia(instanciaComunRepository.getReferenceById(instancia.getCodInstancia()));
        SolicitudInstancia guardada = solicitudRepository.save(solicitud);
        log.info("[AUDIT] Solicitud {} atendida con la instancia {}", idSolicitud, instancia.getIdNegInstancia());
        return solicitudMapper.toDto(guardada);
    }

    @PreAuthorize("hasAuthority('ATENDER_SOLICITUD_INSTANCIA')")
    @Transactional
    public SolicitudInstanciaResponseDTO rechazar(int idSolicitud, RechazarSolicitudInstanciaDTO dto) {
        SolicitudInstancia solicitud = buscarPendienteParaResolver(idSolicitud);
        resolver(solicitud, EstadoSolicitud.RECHAZADA, dto.getRespuesta());
        SolicitudInstancia guardada = solicitudRepository.save(solicitud);
        log.info("[AUDIT] Solicitud {} rechazada", idSolicitud);
        return solicitudMapper.toDto(guardada);
    }

    // =========================================================
    // MÉTODOS PRIVADOS
    // =========================================================

    // Lock pesimista: dos funcionarios atendiendo la misma solicitud a la vez generarían dos instancias.
    private SolicitudInstancia buscarPendienteParaResolver(int idSolicitud) {
        SolicitudInstancia solicitud = solicitudRepository.findByIdParaActualizar(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Solicitud no encontrada: " + idSolicitud));
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya fue resuelta.");
        }
        return solicitud;
    }

    private void resolver(SolicitudInstancia solicitud, EstadoSolicitud estado, String respuesta) {
        Funcionario funcionario = funcionarioRepository.findById(getIdUsuarioAutenticado())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el funcionario autenticado"));
        solicitud.setEstado(estado);
        solicitud.setFuncionario(funcionario);
        solicitud.setRespuesta(respuesta != null && !respuesta.isBlank() ? respuesta.trim() : null);
        solicitud.setFechaResolucion(LocalDateTime.now());
    }

    private SolicitudInstancia buscarOFallar(int idSolicitud) {
        return solicitudRepository.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Solicitud no encontrada: " + idSolicitud));
    }

    private boolean tienePermiso(String permiso) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a -> permiso.equals(a.getAuthority()));
    }

    private int getIdUsuarioAutenticado() {
        return Integer.parseInt(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
