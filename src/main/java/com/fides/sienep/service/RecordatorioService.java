package com.fides.sienep.service;

import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.mapper.RecordatorioMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.*;
import com.fides.sienep.utils.RecordatorioIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

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
    private final FuncionarioGcalRepository funcionarioGcalRepository;
    private final GoogleCalendarService     googleCalendarService;
    private final EmailService              emailService;

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_RECORDATORIO')")
    public RecordatorioResponseDTO crear(RecordatorioRequestDTO dto) {
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

        Funcionario responsable = funcionarioRepository.findById(dto.getIdResponsable())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un funcionario con el id " + dto.getIdResponsable()));

        if (responsable.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "El funcionario con id " + dto.getIdResponsable() + " no está activo");
        }

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

        // ── RF22: sincronizar con Google Calendar si el responsable tiene integración activa ──
        // No es bloqueante: si Google falla, el recordatorio ya fue persistido correctamente.
        String eventIdGoogle = null;
        try {
            Optional<FuncionarioGcal> gcalOpt = funcionarioGcalRepository
                    .findByFuncionarioIdUsuario(responsable.getIdUsuario())
                    .filter(FuncionarioGcal::isAutorizado);
            if (gcalOpt.isPresent()) {
                eventIdGoogle = googleCalendarService.crearEvento(guardado, gcalOpt.get().getRefreshToken());
            }
        } catch (Exception e) {
            log.warn("[GCAL] Fallo al sincronizar recordatorio {} con Google Calendar: {}",
                    guardado.getIdNegRecordatorio(), e.getMessage());
        }

        if (eventIdGoogle != null) {
            try {
                guardado.setEventIdGoogle(eventIdGoogle);
                recordatorioRepository.save(guardado);
            } catch (Exception e) {
                log.error("[GCAL-DESINCRONIZADO] Se creó el evento {} en Google Calendar para el recordatorio {}, pero falló su persistencia local: {}",
                        eventIdGoogle, guardado.getIdNegRecordatorio(), e.getMessage());
            }
        }

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
    public List<RecordatorioResponseDTO> listarActivos() {
        return recordatorioMapper.toDtoList(
                recordatorioRepository.findByEstado(EstadoLogico.ACTIVO));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public RecordatorioResponseDTO buscarPorId(int id) {
        return recordatorioMapper.toDto(buscarActivoOFallar(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public List<RecordatorioResponseDTO> listarPorInstancia(int codInstancia) {
        return recordatorioMapper.toDtoList(
                recordatorioRepository.findByInstanciaAndEstado(codInstancia, EstadoLogico.ACTIVO));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public List<RecordatorioResponseDTO> listarPorResponsable(int idResponsable) {
        return recordatorioMapper.toDtoList(
                recordatorioRepository.findByResponsableAndEstado(idResponsable, EstadoLogico.ACTIVO));
    }

    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_RECORDATORIO')")
    public RecordatorioResponseDTO actualizar(int id, RecordatorioRequestDTO dto) {
        Recordatorio recordatorio = buscarActivoOFallar(id);

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

        Funcionario responsable = funcionarioRepository.findById(dto.getIdResponsable())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un funcionario con el id " + dto.getIdResponsable()));

        if (responsable.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "El funcionario con id " + dto.getIdResponsable() + " no está activo");
        }

        // Se capturan antes de sobrescribir: hacen falta para decidir cómo sincronizar
        // con Google Calendar y si corresponde volver a notificar (RF22/RF23).
        Funcionario responsableAnterior = recordatorio.getResponsable();
        String eventIdAnterior = recordatorio.getEventIdGoogle();
        boolean cambioFecha = !recordatorio.getFechaHora().equals(dto.getFechaHora());

        recordatorio.setTitulo(dto.getTitulo());
        recordatorio.setTipo(dto.getTipo());
        recordatorio.setFechaHora(dto.getFechaHora());
        recordatorio.setInstancia(instancia);
        recordatorio.setCategoria(categoria);
        recordatorio.setResponsable(responsable);
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
        // No es bloqueante, mismo criterio que en crear()/darDeBaja().
        sincronizarEdicionConGoogleCalendar(actualizado, responsableAnterior, eventIdAnterior);

        return recordatorioMapper.toDto(actualizado);
    }

    private void sincronizarEdicionConGoogleCalendar(Recordatorio recordatorio,
                                                      Funcionario responsableAnterior,
                                                      String eventIdAnterior) {
        String nuevoEventIdGoogle = null;
        try {
            boolean cambioResponsable = responsableAnterior.getIdUsuario() != recordatorio.getResponsable().getIdUsuario();
            String eventIdParaActualizar = eventIdAnterior;

            // El evento vive en el calendario del responsable ANTERIOR: si cambió, no se
            // puede "actualizar" con el token del responsable nuevo, hay que borrarlo ahí
            // y (más abajo) crear uno nuevo en el calendario del responsable actual.
            if (cambioResponsable && eventIdAnterior != null) {
                funcionarioGcalRepository
                        .findByFuncionarioIdUsuario(responsableAnterior.getIdUsuario())
                        .filter(FuncionarioGcal::isAutorizado)
                        .ifPresent(gcal -> googleCalendarService.eliminarEvento(eventIdAnterior, gcal.getRefreshToken()));
                recordatorio.setEventIdGoogle(null);
                eventIdParaActualizar = null;
            }

            Optional<FuncionarioGcal> gcalActual = funcionarioGcalRepository
                    .findByFuncionarioIdUsuario(recordatorio.getResponsable().getIdUsuario())
                    .filter(FuncionarioGcal::isAutorizado);

            if (gcalActual.isEmpty()) {
                return;
            }

            String refreshToken = gcalActual.get().getRefreshToken();
            if (eventIdParaActualizar != null) {
                googleCalendarService.actualizarEvento(recordatorio, eventIdParaActualizar, refreshToken);
            } else {
                // No tenía evento (nunca se sincronizó, o cambió de responsable): se crea uno nuevo.
                nuevoEventIdGoogle = googleCalendarService.crearEvento(recordatorio, refreshToken);
            }
        } catch (Exception e) {
            log.warn("[GCAL] Fallo al sincronizar la edición del recordatorio {} con Google Calendar: {}",
                    recordatorio.getIdNegRecordatorio(), e.getMessage());
        }

        if (nuevoEventIdGoogle != null) {
            try {
                recordatorio.setEventIdGoogle(nuevoEventIdGoogle);
                recordatorioRepository.save(recordatorio);
            } catch (Exception e) {
                log.error("[GCAL-DESINCRONIZADO] Se creó el nuevo evento {} en Google Calendar para la edición del recordatorio {}, pero falló su persistencia local: {}",
                        nuevoEventIdGoogle, recordatorio.getIdNegRecordatorio(), e.getMessage());
            }
        }
    }


    @Transactional
    @PreAuthorize("hasAuthority('BAJA_RECORDATORIO')")
    public void darDeBaja(int id) {
        Recordatorio recordatorio = buscarActivoOFallar(id);

        // ── RF22: eliminar evento de Google Calendar si existe ────────────────
        // Igualmente no bloqueante.
        if (recordatorio.getEventIdGoogle() != null) {
            try {
                funcionarioGcalRepository
                        .findByFuncionarioIdUsuario(recordatorio.getResponsable().getIdUsuario())
                        .filter(FuncionarioGcal::isAutorizado)
                        .ifPresent(gcal ->
                                googleCalendarService.eliminarEvento(
                                        recordatorio.getEventIdGoogle(),
                                        gcal.getRefreshToken()
                                )
                        );
            } catch (Exception e) {
                log.warn("[GCAL] No se pudo eliminar el evento {} de Google Calendar: {}",
                        recordatorio.getEventIdGoogle(), e.getMessage());
            }
        }

        recordatorio.setEstado(EstadoLogico.INACTIVO);
        recordatorioRepository.save(recordatorio);
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
    @PreAuthorize("hasAuthority('EDITAR_RECORDATORIO')")
    public RecordatorioResponseDTO reactivar(int id) {
        Recordatorio recordatorio = recordatorioRepository.findByCodRecordatorio(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró un recordatorio con el id " + id));

        if (recordatorio.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El recordatorio ya se encuentra activo");
        }

        // Mismas validaciones que crear()/actualizar(): mientras estuvo INACTIVO, la
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
        return recordatorioMapper.toDto(recordatorioRepository.save(recordatorio));
    }
}