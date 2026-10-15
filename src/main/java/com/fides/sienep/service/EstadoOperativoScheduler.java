package com.fides.sienep.service;

import com.fides.sienep.repository.RecordatorioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// estado_operativo (HOY/FUTURO/VENCIDO) depende de la fecha actual, pero el trigger
// trg_actualizar_estado_recordatorio solo lo calcula cuando se escribe la fila: sin
// este recálculo, un recordatorio que nadie edita se queda en FUTURO para siempre y
// ReporteService nunca cuenta vencidos.
//
// El estado solo cambia al cambiar el día, así que alcanza con correr a medianoche.
// También corre al arrancar, por si el servidor estaba apagado a esa hora.
@Slf4j
@Component
@RequiredArgsConstructor
public class EstadoOperativoScheduler {

    private final RecordatorioRepository recordatorioRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "America/Montevideo")
    public void recalcularEstadosOperativos() {
        int actualizados = recordatorioRepository.recalcularEstadosOperativos();
        log.info("[Recordatorios] estado_operativo recalculado en {} recordatorio(s).", actualizados);
    }

    // Un fallo acá no debe impedir que la aplicación arranque (ej. una base sin
    // proyecto.recalcular_estados_operativos(), como el H2 de los tests): se loguea y
    // la corrida de medianoche lo vuelve a intentar.
    @EventListener(ApplicationReadyEvent.class)
    public void recalcularAlArrancar() {
        try {
            recalcularEstadosOperativos();
        } catch (Exception e) {
            log.warn("[Recordatorios] No se pudo recalcular estado_operativo al arrancar: {}", e.getMessage());
        }
    }
}
