package com.fides.sienep.service;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.repository.RecordatorioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// RF23 — Notificaciones de Recordatorios (cron propio, sin depender de los
// recordatorios automáticos de Google Calendar).
//
// Corre una vez por hora. Por cada Recordatorio ACTIVO cuya fechaHora cae dentro
// de las próximas 24 horas y que todavía no fue notificado, manda un email al
// responsable y marca notificadoProximidad = true para no repetir el envío en la
// próxima corrida.
//
// Ventana de una hora de margen: el cron corre en :00 de cada hora, así que un
// recordatorio "próximo a vencer" puede quedar hasta ~59 minutos sin notificar
// en el peor caso. Es aceptable para el alcance del proyecto; si se necesitara
// precisión mayor, bajar la frecuencia del cron (fixedRate) en vez de la ventana.
@Slf4j
@Component
@RequiredArgsConstructor
public class RecordatorioNotificacionScheduler {

    private static final int VENTANA_HORAS_ANTICIPACION = 24;

    private final RecordatorioRepository recordatorioRepository;
    private final EmailService emailService;

    // Corre a los :00 de cada hora. Formato cron de Spring: seg min hora día mes díaSemana.
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void notificarRecordatoriosProximos() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = ahora.plusHours(VENTANA_HORAS_ANTICIPACION);

        List<Recordatorio> proximos = recordatorioRepository
                .findByEstadoAndNotificadoProximidadFalseAndFechaHoraBetween(
                        EstadoLogico.ACTIVO, ahora, limite);

        if (proximos.isEmpty()) {
            log.info("[RF23] Sin recordatorios próximos a vencer en esta corrida.");
            return;
        }

        log.info("[RF23] {} recordatorio(s) próximos a vencer, enviando notificaciones.", proximos.size());

        for (Recordatorio recordatorio : proximos) {
            try {
                emailService.enviarRecordatorioProximo(
                        recordatorio.getResponsable().getEmail(),
                        recordatorio.getResponsable().getNombre(),
                        recordatorio.getIdNegRecordatorio(),
                        recordatorio.getTitulo(),
                        recordatorio.getFechaHora()
                );
                // Se marca como notificado incluso si el email falla puertas adentro
                // de EmailService (que ya loguea warn y no relanza): reintentar cada
                // hora un envío que sigue fallando no aporta nada y satura los logs.
                recordatorio.setNotificadoProximidad(true);
                recordatorioRepository.save(recordatorio);
            } catch (Exception e) {
                // Si algo inesperado revienta acá (ej. el responsable
                // fue eliminado físicamente y la relación quedó rota), no debe cortar
                // el resto de la corrida ni tirar abajo el @Scheduled.
                log.warn("[RF23] Fallo al notificar recordatorio {} próximo a vencer: {}",
                        recordatorio.getIdNegRecordatorio(), e.getMessage());
            }
        }
    }
}