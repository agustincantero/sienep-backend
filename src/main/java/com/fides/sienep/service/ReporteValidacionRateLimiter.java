package com.fides.sienep.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Limita cuántas veces se puede llamar a POST /api/reportes/validar desde una
// misma IP en una ventana de tiempo. A diferencia de LoginIpRateLimiter, este
// endpoint es público (sin JWT) y no protege ninguna cuenta — el objetivo es
// solo frenar un flood de archivos de hasta 5MB, no bloquear intentos de fuerza
// bruta. Por eso el umbral es más generoso.
//
// Limitación conocida y aceptada para el alcance del proyecto (mismo criterio que
// LoginIpRateLimiter/PasswordResetRateLimiter): el store es en memoria del
// proceso y no se comparte entre instancias si el backend llegara a escalar
// horizontalmente.
@Slf4j
@Component
public class ReporteValidacionRateLimiter {

    private static final int  MAX_INTENTOS    = 15;
    private static final long VENTANA_MINUTOS = 5;

    private final Map<String, List<LocalDateTime>> intentosPorIp = new ConcurrentHashMap<>();

    // Devuelve true si el intento está permitido (y lo registra); false si ya se
    // alcanzó el máximo de intentos para esta IP dentro de la ventana. Con ip null
    // (no se pudo resolver la IP del request) no hay forma de aplicar el límite,
    // así que no bloquea.
    public synchronized boolean registrarIntento(String ip) {
        if (ip == null) {
            return true;
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limiteVentana = ahora.minusMinutes(VENTANA_MINUTOS);

        List<LocalDateTime> intentos = intentosPorIp.get(ip);
        if (intentos == null) {
            intentos = new ArrayList<>();
            intentosPorIp.put(ip, intentos);
        }

        Iterator<LocalDateTime> it = intentos.iterator();
        while (it.hasNext()) {
            if (it.next().isBefore(limiteVentana)) {
                it.remove();
            }
        }

        if (intentos.size() >= MAX_INTENTOS) {
            return false;
        }
        intentos.add(ahora);
        return true;
    }

    // Housekeeping: purga IPs sin intentos recientes para no acumular memoria
    // indefinidamente (mismo criterio que LoginIpRateLimiter.limpiarExpirados()).
    @Scheduled(cron = "0 0 * * * *")
    public void limpiarExpirados() {
        LocalDateTime limiteVentana = LocalDateTime.now().minusMinutes(VENTANA_MINUTOS);
        int purgados = 0;
        Iterator<Map.Entry<String, List<LocalDateTime>>> it = intentosPorIp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<LocalDateTime>> entry = it.next();
            List<LocalDateTime> intentos = entry.getValue();

            Iterator<LocalDateTime> itIntentos = intentos.iterator();
            while (itIntentos.hasNext()) {
                if (itIntentos.next().isBefore(limiteVentana)) {
                    itIntentos.remove();
                }
            }

            if (intentos.isEmpty()) {
                it.remove();
                purgados++;
            }
        }
        if (purgados > 0) {
            log.info("[RATE-LIMIT] {} entrada(s) de rate-limit de validación de reportes purgadas", purgados);
        }
    }
}
