package com.fides.sienep.service;

import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.event.AccionCalendar;
import com.fides.sienep.event.CalendarRecordatorioEvent;
import com.fides.sienep.mapper.RecordatorioMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.*;
import com.fides.sienep.utils.RecordatorioIdGenerator;
import com.fides.sienep.utils.RecurrenceRuleBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
public class RecordatorioService {

    private final RecordatorioRepository recordatorioRepository;
    private final InstanciaRepository instanciaRepository;
    private final CategoriaRecordatorioRepository categoriaRecordatorioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FrecuenciaRepository frecuenciaRepository;
    private final RecordatorioMapper recordatorioMapper;
    private final RecordatorioIdGenerator recordatorioIdGenerator;
    private final RecurrenceRuleBuilder recurrenceRuleBuilder;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService emailService;

    // Mismo criterio que InstanciaComunService e IncidenciaService: el JWT trae el idUsuario
    // como subject, y es la única fuente confiable de quién está operando.
    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(auth.getName());
    }

    // El responsable es siempre quien crea el recordatorio, tomado de la sesión y no del
    // body (igual que el funcionario de una instancia): así nadie puede agendar un
    // recordatorio a nombre de otro llamando a la API directamente.
    @Transactional
    @PreAuthorize("hasAuthority('CREAR_RECORDATORIO')")
    public RecordatorioResponseDTO crear(RecordatorioRequestDTO dto) {
        return crearInterno(dto, getIdUsuarioAutenticado());
    }

    // Misma lógica que crear(), pero sin @Transactional ni @PreAuthorize propios: existe
    // para que InstanciaComunService.crear() (recordatorio inicial opcional) la llame dentro de
    // SU PROPIA transacción en vez de a través del proxy de crear(). Si llamara a crear() y
    // este lanzara una excepción, el interceptor de @Transactional de Spring marca la
    // transacción ambiente como rollback-only ANTES de que ningún catch de más arriba pueda
    // hacer nada — aunque se atrape la excepción, la instancia ya guardada se revertiría
    // igual al hacer commit. Este método, al no tener su propio @Transactional, no cruza
    // ese límite de proxy y una excepción acá puede atraparse sin efecto sobre la
    // transacción del llamador. idResponsable lo resuelve quien llama a partir de la sesión
    // (crear() o el funcionario creador de la instancia), nunca del DTO.
    RecordatorioResponseDTO crearInterno(RecordatorioRequestDTO dto, int idResponsable) {
        Instancia instancia = instanciaRepository.findById(dto.getCodInstancia())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una instancia con el id " + dto.getCodInstancia()));

        if (instancia.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "La instancia con id " + dto.getCodInstancia() + " no está activa");
        }

        CategoriaRecordatorio categoria = categoriaRecordatorioRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        if (categoria.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "La categoría con id " + dto.getIdCategoria() + " no está activa");
        }

        Funcionario responsable = buscarFuncionarioAutenticadoActivo(idResponsable);

        Recordatorio recordatorio = new Recordatorio(
                dto.getTitulo(),
                dto.getTipo(),
                dto.getFechaHora(),
                instancia,
                categoria,
                responsable
        );

        recordatorio.setIdNegRecordatorio(recordatorioIdGenerator.generarIdNeg());

        if (dto.getIdFrecuencia() != null) {
            Frecuencia frecuencia = frecuenciaRepository.findById(dto.getIdFrecuencia())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No se encontró una frecuencia con el id " + dto.getIdFrecuencia()));

            if (frecuencia.getEstado() != EstadoLogico.ACTIVO) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "La frecuencia con id " + dto.getIdFrecuencia() + " no está activa");
            }
            recordatorio.setFrecuencia(frecuencia);
        }

        recordatorio.setFechaFin(dto.getFechaFin());
        recordatorio.setNroOcurrencias(dto.getNroOcurrencias());

        Recordatorio guardado = recordatorioRepository.save(recordatorio);

        // ── RF22: sincronizar con Google Calendar (async, post-commit) ──
        // A diferencia del modelo anterior (OAuth por funcionario), ya no depende de que
        // el responsable haya autorizado nada: la sync es incondicional, vía Service
        // Account. El rrule se calcula acá (síncrono, con la sesión de Hibernate todavía
        // abierta) porque CalendarSyncListener corre después del commit, cuando ya no
        // se puede tocar guardado.getFrecuencia() sin arriesgar un LazyInitializationException.
        String rrule = recurrenceRuleBuilder.construir(guardado);
        eventPublisher.publishEvent(CalendarRecordatorioEvent.builder()
                .idRecordatorio(guardado.getCodRecordatorio())
                .idNegRecordatorio(guardado.getIdNegRecordatorio())
                .accion(AccionCalendar.CREAR)
                .titulo(guardado.getTitulo())
                .descripcion(descripcionCalendar(guardado.getIdNegRecordatorio(), categoria, guardado.getTipo()))
                .fechaHora(guardado.getFechaHora())
                .rrule(rrule)
                .emailFuncionario(responsable.getEmail())
                .nombreFuncionario(responsable.getNombre())
                .build());

        // ── RF23: notificar por email al responsable del recordatorio ──
        // No es bloqueante: enviarNotificacionRecordatorio() ya atrapa sus propias excepciones,
        // este try/catch es una segunda capa de defensa (mismo criterio que RF15 en InstanciaComunService).
        try {
            emailService.enviarNotificacionRecordatorio(
                    responsable.getEmail(),
                    responsable.getNombre(),
                    guardado.getIdNegRecordatorio(),
                    guardado.getTitulo(),
                    guardado.getFechaHora()
            );
        } catch (Exception e) {
            log.warn("[AUDIT] Fallo al notificar recordatorio {} al responsable {}: {}",
                    guardado.getIdNegRecordatorio(), responsable.getIdUsuario(), e.getMessage());
        }

        return recordatorioMapper.toDto(guardado);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public Page<RecordatorioResponseDTO> listar(EstadoLogico estado, Pageable pageable) {
        EstadoLogico estadoEfectivo = estado != null ? estado : EstadoLogico.ACTIVO;
        Page<Recordatorio> pagina = recordatorioRepository.findByEstado(estadoEfectivo, pageable);
        List<RecordatorioResponseDTO> dtos = recordatorioMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public RecordatorioResponseDTO obtenerPorId(int id) {
        return recordatorioMapper.toDto(buscarActivoOFallar(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public Page<RecordatorioResponseDTO> listarPorInstancia(int codInstancia, EstadoLogico estado, Pageable pageable) {
        EstadoLogico estadoEfectivo = estado != null ? estado : EstadoLogico.ACTIVO;
        Page<Recordatorio> pagina = recordatorioRepository.findByInstanciaAndEstado(
                codInstancia, estadoEfectivo, pageable);
        List<RecordatorioResponseDTO> dtos = recordatorioMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_RECORDATORIO')")
    public RecordatorioResponseDTO editar(int id, RecordatorioRequestDTO dto) {
        Recordatorio recordatorio = buscarActivoOFallar(id);

        // codInstancia es inmutable después de creado: no hay ningún caso de uso
        // real que necesite "mover" un recordatorio a otra instancia (que podría
        // ser hasta de otro estudiante). Si en el futuro aparece un caso de uso
        // real (ej. mover entre instancias del mismo estudiante), agregar esa
        // validación puntual en vez de sacar este chequeo.
        if (dto.getCodInstancia() != recordatorio.getInstancia().getCodInstancia()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede reasignar un recordatorio a otra instancia. "
                            + "Si el recordatorio está mal asociado, eliminelo y créelo de nuevo bajo la instancia correcta.");
        }

        Instancia instancia = instanciaRepository.findById(dto.getCodInstancia())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una instancia con el id " + dto.getCodInstancia()));

        if (instancia.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "La instancia con id " + dto.getCodInstancia() + " no está activa");
        }

        CategoriaRecordatorio categoria = categoriaRecordatorioRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        if (categoria.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "La categoría con id " + dto.getIdCategoria() + " no está activa");
        }

        // El responsable no se edita: se conserva el que se fijó al crearlo (quien lo creó).

        // Se captura antes de sobrescribir: hace falta para decidir si corresponde
        // volver a notificar (RF23) y para el ACTUALIZAR/CREAR de Google Calendar (RF22).
        String eventIdAnterior = recordatorio.getEventIdGoogle();
        boolean cambioFecha = !recordatorio.getFechaHora().equals(dto.getFechaHora());

        recordatorio.setTitulo(dto.getTitulo());
        recordatorio.setTipo(dto.getTipo());
        recordatorio.setFechaHora(dto.getFechaHora());
        recordatorio.setInstancia(instancia);
        recordatorio.setCategoria(categoria);
        recordatorio.setFechaFin(dto.getFechaFin());
        recordatorio.setNroOcurrencias(dto.getNroOcurrencias());

        if (dto.getIdFrecuencia() != null) {
            Frecuencia frecuencia = frecuenciaRepository.findById(dto.getIdFrecuencia())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No se encontró una frecuencia con el id " + dto.getIdFrecuencia()));

            if (frecuencia.getEstado() != EstadoLogico.ACTIVO) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "La frecuencia con id " + dto.getIdFrecuencia() + " no está activa");
            }
            recordatorio.setFrecuencia(frecuencia);
        } else {
            recordatorio.setFrecuencia(null);
        }

        // RF23 — si la fecha cambió, hay que poder volver a notificar "próximo a vencer"
        // para la nueva fecha; de lo contrario el flag queda pegado en true para siempre
        // y el recordatorio postergado nunca se vuelve a notificar.
        if (cambioFecha) {
            recordatorio.setNotificadoProximidad(false);
        }

        Recordatorio actualizado = recordatorioRepository.save(recordatorio);

        // RF22 — mantener sincronizado el evento de Google Calendar tras la edición.
        // Con Service Account todos los eventos viven en un único calendario compartido,
        // así que a diferencia del modelo anterior (OAuth por funcionario, un calendario
        // personal por responsable) un cambio de responsable ya NO requiere borrar el
        // evento viejo y crear uno nuevo: alcanza con ACTUALIZAR el mismo evento con la
        // lista de asistentes nueva. Si nunca se había sincronizado (eventIdAnterior nulo),
        // se crea.
        String rrule = recurrenceRuleBuilder.construir(actualizado);
        eventPublisher.publishEvent(CalendarRecordatorioEvent.builder()
                .idRecordatorio(actualizado.getCodRecordatorio())
                .idNegRecordatorio(actualizado.getIdNegRecordatorio())
                .oldEventId(eventIdAnterior)
                .accion(eventIdAnterior != null ? AccionCalendar.ACTUALIZAR : AccionCalendar.CREAR)
                .titulo(actualizado.getTitulo())
                .descripcion(descripcionCalendar(actualizado.getIdNegRecordatorio(), actualizado.getCategoria(), actualizado.getTipo()))
                .fechaHora(actualizado.getFechaHora())
                .rrule(rrule)
                .emailFuncionario(actualizado.getResponsable().getEmail())
                .nombreFuncionario(actualizado.getResponsable().getNombre())
                .build());

        return recordatorioMapper.toDto(actualizado);
    }

    @Transactional
    @PreAuthorize("hasAuthority('DESACTIVAR_RECORDATORIO')")
    public void desactivar(int id) {
        Recordatorio recordatorio = buscarActivoOFallar(id);

        // ── RF22: eliminar evento de Google Calendar si existe (async, post-commit) ──
        if (recordatorio.getEventIdGoogle() != null) {
            eventPublisher.publishEvent(CalendarRecordatorioEvent.builder()
                    .idRecordatorio(recordatorio.getCodRecordatorio())
                    .idNegRecordatorio(recordatorio.getIdNegRecordatorio())
                    .oldEventId(recordatorio.getEventIdGoogle())
                    .accion(AccionCalendar.ELIMINAR)
                    .build());
            // Si no se limpia acá, una reactivar() + editar() posterior vería este
            // eventId viejo como no nulo e intentaría ACTUALIZAR un evento que ya no
            // existe en Calendar (fue borrado arriba) en vez de CREAR uno nuevo.
            recordatorio.setEventIdGoogle(null);
        }

        recordatorio.setEstado(EstadoLogico.INACTIVO);
        recordatorioRepository.save(recordatorio);
    }

    // Escritura de vuelta del eventId tras la sync async (CalendarSyncListener,
    // AFTER_COMMIT) — corre en una transacción nueva, la original ya cerró.
    @Transactional
    public void actualizarEventIdGoogle(int id, String eventId) {
        recordatorioRepository.findByCodRecordatorio(id).ifPresent(recordatorio -> {
            recordatorio.setEventIdGoogle(eventId);
            recordatorioRepository.save(recordatorio);
        });
    }

    // Descripción del evento de Google Calendar: ID de negocio (para poder ubicar el
    // recordatorio en el sistema desde la invitación) + categoría + tipo.
    private String descripcionCalendar(String idNegRecordatorio, CategoriaRecordatorio categoria, TipoRecordatorio tipo) {
        return "ID: " + idNegRecordatorio + " — Categoría: " + categoria.getNomCategoria() + " — Tipo: " + tipo;
    }

    private Recordatorio buscarActivoOFallar(int id) {
        return recordatorioRepository.findByCodRecordatorioAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un recordatorio activo con el id " + id));
    }

    // RF27 — Se llama desde InstanciaComunService.crearDesdeRecordatorio() una vez que la
    // instancia de seguimiento ya fue creada exitosamente. Deliberadamente NO conoce nada
    // de InstanciaComunService ni de Instancia: solo marca el flag. Mantener esta dirección
    // (InstanciaComunService → RecordatorioService) es lo que evita un ciclo de dependencias
    // entre los dos services — si esto alguna vez necesita "saber" sobre instancias, esa
    // lógica va en InstanciaComunService, no acá.
    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_RECORDATORIO')")
    public RecordatorioResponseDTO marcarCumplido(int id) {
        Recordatorio recordatorio = buscarActivoOFallar(id);

        if (recordatorio.isCumplido()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El recordatorio con id " + id + " ya fue marcado como cumplido");
        }

        recordatorio.setCumplido(true);
        return recordatorioMapper.toDto(recordatorioRepository.save(recordatorio));
    }


    @Transactional
    @PreAuthorize("hasAuthority('REACTIVAR_RECORDATORIO')")
    public RecordatorioResponseDTO reactivar(int id) {
        Recordatorio recordatorio = recordatorioRepository.findByCodRecordatorio(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró un recordatorio con el id " + id));

        if (recordatorio.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El recordatorio ya se encuentra activo");
        }

        // Mismas validaciones que crear()/editar(): mientras estuvo INACTIVO, la
        // instancia, categoría o responsable pudieron haberse dado de baja también.
        if (recordatorio.getInstancia().getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "No se puede reactivar: la instancia asociada ya no está activa");
        }
        if (recordatorio.getCategoria().getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "No se puede reactivar: la categoría asociada ya no está activa");
        }
        if (recordatorio.getResponsable().getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "No se puede reactivar: el responsable asociado ya no está activo");
        }

        recordatorio.setEstado(EstadoLogico.ACTIVO);
        Recordatorio reactivado = recordatorioRepository.save(recordatorio);

        // RF22 — desactivar() ya limpió eventIdGoogle al eliminar el evento; sin esto,
        // el recordatorio queda reactivado en el sistema pero sin evento en Calendar
        // hasta que alguien lo edite (que puede no pasar nunca).
        String rrule = recurrenceRuleBuilder.construir(reactivado);
        eventPublisher.publishEvent(CalendarRecordatorioEvent.builder()
                .idRecordatorio(reactivado.getCodRecordatorio())
                .idNegRecordatorio(reactivado.getIdNegRecordatorio())
                .accion(AccionCalendar.CREAR)
                .titulo(reactivado.getTitulo())
                .descripcion(descripcionCalendar(reactivado.getIdNegRecordatorio(), reactivado.getCategoria(), reactivado.getTipo()))
                .fechaHora(reactivado.getFechaHora())
                .rrule(rrule)
                .emailFuncionario(reactivado.getResponsable().getEmail())
                .nombreFuncionario(reactivado.getResponsable().getNombre())
                .build());

        return recordatorioMapper.toDto(reactivado);
    }

    // Mismas validaciones y mensajes que InstanciaComunService.crear() para el funcionario autenticado.
    private Funcionario buscarFuncionarioAutenticadoActivo(int idFuncionario) {
        Funcionario funcionario = funcionarioRepository.findById(idFuncionario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el funcionario autenticado"));

        if (funcionario.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "El funcionario autenticado no está activo");
        }
        return funcionario;
    }
}
