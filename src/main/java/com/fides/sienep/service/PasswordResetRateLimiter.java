package com.fides.sienep.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Limita cuántas veces se puede solicitar restablecer la contraseña para un mismo
// email en una ventana de tiempo. La clave del conteo es el email tal como lo
// mandó el cliente, no el idUsuario: AuthService.olvideMiContrasenia() responde
// siempre lo mismo si el email existe o no ("no revelar si el email existe o no"),
// así que el rate-limit tiene que aplicarse de la misma forma en ambos casos. Si
// solo se contaran los intentos de emails que sí existen, un 429 únicamente ahí
// (y nunca para emails inexistentes) sería en sí mismo un oráculo de enumeración.
//
// Limitación conocida y aceptada para el alcance del proyecto (mismo criterio que
// OAuthStateStore): el store es en memoria del proceso y no se comparte entre
// instancias si el backend llegara a escalar horizontalmente.
@Slf4j
@Component
public class PasswordResetRateLimiter {

    private static final int  MAX_INTENTOS     = 3;
    private static final long VENTANA_MINUTOS  = 15;

    private final Map<String, List<LocalDateTime>> intentosPorEmail = new ConcurrentHashMap<>();

    // Devuelve true si el intento está permitido (y lo registra); false si ya se
    // alcanzó el máximo de intentos para este email dentro de la ventana.
    public synchronized boolean registrarIntento(String email) {
        String clave = email.toLowerCase(Locale.ROOT);
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limiteVentana = ahora.minusMinutes(VENTANA_MINUTOS);

        List<LocalDateTime> intentos = intentosPorEmail.get(clave);
        if (intentos == null) {
            intentos = new ArrayList<>();
            intentosPorEmail.put(clave, intentos);
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

    // Housekeeping: purga emails sin intentos recientes para no acumular memoria
    // indefinidamente (mismo criterio que OAuthStateStore.limpiarExpirados()).
    @Scheduled(cron = "0 0 * * * *")
    public void limpiarExpirados() {
        LocalDateTime limiteVentana = LocalDateTime.now().minusMinutes(VENTANA_MINUTOS);
        int purgados = 0;
        Iterator<Map.Entry<String, List<LocalDateTime>>> it = intentosPorEmail.entrySet().iterator();
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
            log.info("[RATE-LIMIT] {} entrada(s) de rate-limit de restablecimiento purgadas", purgados);
        }
    }
}
