package com.fides.sienep.service;

import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.dto.InstanciaComunUpdateDTO;
import com.fides.sienep.dto.InstanciaDesdeRecordatorioDTO;
import com.fides.sienep.dto.RecordatorioInicialDTO;
import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.event.AccionCalendar;
import com.fides.sienep.event.CalendarInstanciaEvent;
import com.fides.sienep.mapper.InstanciaComunMapper;
import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Instancia;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.repository.CategoriaInstanciaRepository;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.repository.InstanciaComunSpecs;
import com.fides.sienep.repository.RecordatorioRepository;
import com.fides.sienep.utils.InstanciaIdGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstanciaComunService {

    private static final Logger log = LoggerFactory.getLogger(InstanciaComunService.class);

    private final InstanciaComunRepository instanciaComunRepository;
    private final EstudianteRepository estudianteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final CategoriaInstanciaRepository categoriaInstanciaRepository;
    private final InstanciaIdGenerator instanciaIdGenerator;
    private final InstanciaComunMapper instanciaComunMapper;
    private final EmailService emailService;
    private final RecordatorioService recordatorioService;
    private final RecordatorioRepository recordatorioRepository;
    private final ApplicationEventPublisher eventPublisher;

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(auth.getName());
    }

    private boolean tienePermiso(String permiso) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        for (var authority : auth.getAuthorities()) {
            if (authority.getAuthority().equals(permiso)) return true;
        }
        return false;
    }

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_INSTANCIA')")
    public InstanciaComunResponseDTO crear(InstanciaComunRequestDTO dto) {
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

        CategoriaInstancia categoria = categoriaInstanciaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        if (categoria.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "La categoría con id " + dto.getIdCategoria() + " no está activa");
        }

        InstanciaComun instancia = new InstanciaComun(
                dto.getTitulo(),
                dto.getFechaHora(),
                estudiante,
                funcionario,
                dto.getCanal(),
                categoria
        );

        instancia.setIdNegInstancia(instanciaIdGenerator.generarIdNeg());

        InstanciaComunResponseDTO response = instanciaComunMapper.toDto(
                instanciaComunRepository.save(instancia));

        // RF15 — notificar al funcionario creador con el ID de negocio generado.
        // El try-catch externo es una segunda capa de defensa: aunque enviarNotificacionInstancia
        // ya no relanza, esto garantiza que cualquier excepción inesperada no revierta
        // la transacción de @Transactional.
        try {
            emailService.enviarNotificacionInstancia(
                    funcionario.getEmail(),
                    funcionario.getNombre(),
                    response.getIdNegInstancia(),
                    response.getTitulo(),
                    instancia.getFechaHora()
            );
        } catch (Exception e) {
            log.warn("[AUDIT] Fallo al notificar instancia {} al funcionario {}: {}",
                    response.getIdNegInstancia(), funcionario.getIdUsuario(), e.getMessage());
        }

        // RF13 — Integración de Instancias con Google Calendar (async, post-commit). A
        // diferencia de CalendarRecordatorioEvent, acá SÍ se invita al alumno: la instancia
        // representa una cita/reunión real, no agenda interna.
        eventPublisher.publishEvent(CalendarInstanciaEvent.builder()
                .codInstancia(response.getCodInstancia())
                .idNegInstancia(response.getIdNegInstancia())
                .accion(AccionCalendar.CREAR)
                .titulo(instancia.getTitulo())
                .descripcion(descripcionCalendar(response.getIdNegInstancia(), categoria, instancia.getCanal()))
                .fechaHora(instancia.getFechaHora())
                .emailFuncionario(funcionario.getEmail())
                .nombreFuncionario(funcionario.getNombre())
                .emailAlumno(estudiante.getEmail())
                .nombreAlumno(estudiante.getNombre())
                .build());

        // Recordatorio inicial opcional (sin ficha propia en la especificación), con sync a
        // Google Calendar heredado de RF22.
        // Best-effort, mismo criterio que el mail de arriba: la instancia es el registro real
        // del caso, el recordatorio inicial es una comodidad — si falla (categoría inexistente,
        // sin permiso CREAR_RECORDATORIO, etc.) no tiene sentido perder también los datos
        // válidos de la instancia. Por eso se llama a crearInterno() y no a crear(): crear()
        // pasa por el proxy de @Transactional de Spring, que marca la transacción ambiente
        // como rollback-only apenas lanza una excepción, sin importar que se atrape acá — la
        // instancia se revertiría igual al hacer commit. tienePermiso() replica a mano el
        // @PreAuthorize que crearInterno() no tiene (al no pasar por el proxy de crear()).
        if (dto.getRecordatorioInicial() != null) {
            try {
                if (!tienePermiso("CREAR_RECORDATORIO")) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "No tiene permiso para crear el recordatorio inicial");
                }
                RecordatorioRequestDTO recordatorioDto = construirRecordatorioDesdeInstancia(
                        dto.getRecordatorioInicial(), response.getCodInstancia(), funcionario.getIdUsuario());

                RecordatorioResponseDTO recordatorioCreado = recordatorioService.crearInterno(recordatorioDto);
                response.setRecordatorioCreado(recordatorioCreado);
            } catch (Exception e) {
                log.warn("[AUDIT] Fallo al crear el recordatorio inicial de la instancia {}: {}",
                        response.getIdNegInstancia(), e.getMessage());
            }
        }

        return response;
    }

    // Arma el RecordatorioRequestDTO "completo" a partir del RecordatorioInicialDTO
    // (que no trae codInstancia ni, a veces, idResponsable).
    private RecordatorioRequestDTO construirRecordatorioDesdeInstancia(
            RecordatorioInicialDTO origen, int codInstancia, int idFuncionarioCreador) {

        RecordatorioRequestDTO dto = new RecordatorioRequestDTO();
        dto.setTitulo(origen.getTitulo());
        dto.setTipo(origen.getTipo());
        dto.setFechaHora(origen.getFechaHora());
        dto.setCodInstancia(codInstancia);
        dto.setIdCategoria(origen.getIdCategoria());
        dto.setIdResponsable(
                origen.getIdResponsable() != null ? origen.getIdResponsable() : idFuncionarioCreador);
        dto.setIdFrecuencia(origen.getIdFrecuencia());
        dto.setFechaFin(origen.getFechaFin());

        // Defensa en profundidad: este DTO se construye a mano y nunca pasa por @Valid,
        // así que el @Min(1) del RecordatorioInicialDTO no protege este camino.
        // nroOcurrencias=0 junto con una frecuencia produciría una recurrencia sin límite
        // en Google Calendar (regresión en el recordatorio inicial).
        Integer nroOcurrencias = origen.getNroOcurrencias();
        if (nroOcurrencias != null && nroOcurrencias < 1) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "El número de ocurrencias del recordatorio inicial debe ser al menos 1");
        }
        dto.setNroOcurrencias(nroOcurrencias);
        return dto;
    }

    // RF27 — genera una instancia de seguimiento a partir de un recordatorio cumplido.
    // Vive acá y no en RecordatorioService a propósito: InstanciaComunService ya depende
    // de RecordatorioService (desde el recordatorio inicial en crear()), así que agregar la dependencia inversa crearía
    // un ciclo. RecordatorioService.marcarCumplido() no sabe nada de instancias.
    @Transactional
    @PreAuthorize("hasAuthority('CREAR_INSTANCIA')")
    public InstanciaComunResponseDTO crearDesdeRecordatorio(int codRecordatorio, InstanciaDesdeRecordatorioDTO dto) {
        // Lock pesimista: evita que dos requests concurrentes para el mismo
        // recordatorio pasen ambas el chequeo de "no cumplido" de abajo antes de
        // que cualquiera lo marque, lo que generaría instancias de seguimiento
        // duplicadas (ver comentario en RecordatorioRepository).
        Recordatorio recordatorio = recordatorioRepository
                .findByCodRecordatorioAndEstadoParaActualizar(codRecordatorio, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un recordatorio activo con el id " + codRecordatorio));

        if (recordatorio.isCumplido()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El recordatorio con id " + codRecordatorio + " ya generó una instancia");
        }

        Instancia instanciaOrigen = recordatorio.getInstancia();
        Estudiante estudiante = instanciaOrigen.getEstudiante();

        InstanciaComunRequestDTO instanciaDto = new InstanciaComunRequestDTO();
        instanciaDto.setTitulo(
                dto.getTitulo() != null && !dto.getTitulo().isBlank()
                        ? dto.getTitulo()
                        : "Seguimiento: " + recordatorio.getTitulo());
        instanciaDto.setFechaHora(LocalDateTime.now());
        instanciaDto.setIdEstudiante(estudiante.getIdUsuario());
        instanciaDto.setCanal(dto.getCanal());
        instanciaDto.setIdCategoria(dto.getIdCategoria());

        // Self-invocation intencional: this.crear() no pasa por el proxy de Spring, así que
        // su propio @Transactional/@PreAuthorize no se re-evalúan acá — corre como método
        // plano, dentro de la misma transacción que ya abrió crearDesdeRecordatorio(), y la
        // autorización ya se validó arriba. Es el comportamiento buscado, no un bug: si
        // alguna vez esto se refactoriza a una clase separada, hay que revisar este detalle.
        InstanciaComunResponseDTO nuevaInstancia = this.crear(instanciaDto);

        // Se marca cumplido recién después de que la instancia se creó con éxito: si crear()
        // lanza una excepción, el rollback de @Transactional deshace todo y el recordatorio
        // queda sin marcar, disponible para reintentar.
        recordatorioService.marcarCumplido(codRecordatorio);

        return nuevaInstancia;
    }

    // Todos los filtros que lleguen se combinan (AND). Antes había un método por filtro (listarPorEstudiante, por
    // funcionario, por categoría) y el controller usaba solo el primero presente: estudiante + categoría devolvía
    // todas las instancias del estudiante, sin filtrar por categoría.
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public Page<InstanciaComunResponseDTO> listar(Integer idEstudiante, Integer idFuncionario, Integer idCategoria,
                                                  EstadoLogico estado, Pageable pageable) {
        EstadoLogico estadoEfectivo = estado != null ? estado : EstadoLogico.ACTIVO;

        Specification<InstanciaComun> spec = InstanciaComunSpecs.estado(estadoEfectivo);
        if (idEstudiante != null) spec = spec.and(InstanciaComunSpecs.estudiante(idEstudiante));
        if (idFuncionario != null) spec = spec.and(InstanciaComunSpecs.funcionario(idFuncionario));
        if (idCategoria != null) spec = spec.and(InstanciaComunSpecs.categoria(idCategoria));

        Page<InstanciaComun> pagina = instanciaComunRepository.findAll(spec, pageable);
        List<InstanciaComunResponseDTO> dtos = instanciaComunMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public InstanciaComunResponseDTO obtenerPorId(int codInstancia) {
        return instanciaComunMapper.toDto(buscarActivaOFallar(codInstancia));
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public Page<InstanciaComunResponseDTO> listarMisInstancias(Pageable pageable) {
        Integer idEstudiante = getIdUsuarioAutenticado();
        Page<InstanciaComun> pagina = instanciaComunRepository.findByEstudianteAndEstado(
                idEstudiante, EstadoLogico.ACTIVO, pageable);
        List<InstanciaComunResponseDTO> dtos = instanciaComunMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_INSTANCIA')")
    public InstanciaComunResponseDTO editar(int codInstancia, InstanciaComunUpdateDTO dto) {
        InstanciaComun instancia = buscarActivaOFallar(codInstancia);

        CategoriaInstancia categoria = categoriaInstanciaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        if (categoria.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "La categoría con id " + dto.getIdCategoria() + " no está activa");
        }

        // Se captura antes de sobrescribir: hace falta para decidir ACTUALIZAR vs
        // CREAR el evento de Google Calendar.
        String eventIdAnterior = instancia.getEventIdGoogle();

        instancia.setTitulo(dto.getTitulo());
        instancia.setFechaHora(dto.getFechaHora());
        instancia.setCanal(dto.getCanal());
        instancia.setCategoria(categoria);

        InstanciaComun actualizada = instanciaComunRepository.save(instancia);

        eventPublisher.publishEvent(CalendarInstanciaEvent.builder()
                .codInstancia(actualizada.getCodInstancia())
                .idNegInstancia(actualizada.getIdNegInstancia())
                .oldEventId(eventIdAnterior)
                .accion(eventIdAnterior != null ? AccionCalendar.ACTUALIZAR : AccionCalendar.CREAR)
                .titulo(actualizada.getTitulo())
                .descripcion(descripcionCalendar(actualizada.getIdNegInstancia(), categoria, actualizada.getCanal()))
                .fechaHora(actualizada.getFechaHora())
                .emailFuncionario(actualizada.getFuncionario().getEmail())
                .nombreFuncionario(actualizada.getFuncionario().getNombre())
                .emailAlumno(actualizada.getEstudiante().getEmail())
                .nombreAlumno(actualizada.getEstudiante().getNombre())
                .build());

        return instanciaComunMapper.toDto(actualizada);
    }

    @Transactional
    @PreAuthorize("hasAuthority('DESACTIVAR_INSTANCIA')")
    public void desactivar(int codInstancia) {
        InstanciaComun instancia = buscarActivaOFallar(codInstancia);

        if (instancia.getEventIdGoogle() != null) {
            eventPublisher.publishEvent(CalendarInstanciaEvent.builder()
                    .codInstancia(instancia.getCodInstancia())
                    .idNegInstancia(instancia.getIdNegInstancia())
                    .oldEventId(instancia.getEventIdGoogle())
                    .accion(AccionCalendar.ELIMINAR)
                    .build());
            // Si no se limpia acá, una reactivar() + editar() posterior vería este
            // eventId viejo como no nulo e intentaría ACTUALIZAR un evento que ya no
            // existe en Calendar (fue borrado arriba) en vez de CREAR uno nuevo.
            instancia.setEventIdGoogle(null);
        }

        instancia.setEstado(EstadoLogico.INACTIVO);
        instanciaComunRepository.save(instancia);
    }

    // Escritura de vuelta del eventId tras la sync async (CalendarSyncListener,
    // AFTER_COMMIT) — corre en una transacción nueva, la original ya cerró.
    @Transactional
    public void actualizarEventIdGoogle(int codInstancia, String eventId) {
        instanciaComunRepository.findById(codInstancia).ifPresent(instancia -> {
            instancia.setEventIdGoogle(eventId);
            instanciaComunRepository.save(instancia);
        });
    }

    @Transactional
    @PreAuthorize("hasAuthority('REACTIVAR_INSTANCIA')")
    public InstanciaComunResponseDTO reactivar(int codInstancia) {
        InstanciaComun instancia = instanciaComunRepository.findById(codInstancia)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró una instancia con el id " + codInstancia));

        if (instancia.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La instancia ya se encuentra activa");
        }

        instancia.setEstado(EstadoLogico.ACTIVO);
        InstanciaComun guardada = instanciaComunRepository.save(instancia);

        // RF13 — desactivar() ya limpió eventIdGoogle al eliminar el evento; sin esto,
        // la instancia queda reactivada en el sistema pero sin evento en Calendar
        // hasta que alguien la edite (que puede no pasar nunca).
        eventPublisher.publishEvent(CalendarInstanciaEvent.builder()
                .codInstancia(guardada.getCodInstancia())
                .idNegInstancia(guardada.getIdNegInstancia())
                .accion(AccionCalendar.CREAR)
                .titulo(guardada.getTitulo())
                .descripcion(descripcionCalendar(guardada.getIdNegInstancia(), guardada.getCategoria(), guardada.getCanal()))
                .fechaHora(guardada.getFechaHora())
                .emailFuncionario(guardada.getFuncionario().getEmail())
                .nombreFuncionario(guardada.getFuncionario().getNombre())
                .emailAlumno(guardada.getEstudiante().getEmail())
                .nombreAlumno(guardada.getEstudiante().getNombre())
                .build());

        return instanciaComunMapper.toDto(guardada);
    }

    // ----------------------------------------------------------------
    // Helpers privados
    // ----------------------------------------------------------------

    // Descripción del evento de Google Calendar: ID de negocio (para poder ubicar la
    // instancia en el sistema desde la invitación) + categoría + canal.
    private String descripcionCalendar(String idNegInstancia, CategoriaInstancia categoria, String canal) {
        String base = "ID: " + idNegInstancia + " — Categoría: " + categoria.getNomCategoria();
        return canal != null && !canal.isBlank() ? base + " — Canal: " + canal : base;
    }

    private InstanciaComun buscarActivaOFallar(int codInstancia) {
        return instanciaComunRepository.findByCodInstanciaAndEstado(codInstancia, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una instancia activa con el id " + codInstancia));
    }
}