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

// Limita los intentos de login FALLIDOS por IP de origen (independientemente de la
// cuenta contra la que se intente), para frenar password-spraying distribuido: un
// atacante que prueba credenciales contra muchas cuentas distintas desde la misma
// IP nunca alcanza el bloqueo por cuenta (5 fallos por cuenta en AuthService), pero
// sí este límite por IP.
//
// El umbral es bastante más alto que el de por-cuenta a propósito: varias cuentas
// legítimas pueden compartir la misma IP pública (NAT de una red de campus/oficina),
// y ahí los intentos fallidos "normales" (contraseñas mal tipeadas) de mucha gente
// distinta pueden acumularse rápido. Un umbral bajo bloquearía a toda esa red por
// el error de una sola persona.
//
// Limitación conocida y aceptada para el alcance del proyecto (mismo criterio que
// PasswordResetRateLimiter): el store es en memoria
// del proceso y no se comparte entre instancias si el backend llegara a escalar
// horizontalmente.
@Slf4j
@Component
public class LoginIpRateLimiter {

    private static final int  MAX_INTENTOS_FALLIDOS = 20;
    private static final long VENTANA_MINUTOS       = 10;

    private final Map<String, List<LocalDateTime>> fallosPorIp = new ConcurrentHashMap<>();

    // Registra un intento de login fallido para esta IP. Devuelve true si todavía
    // está dentro del límite (y lo registra); false si ya se alcanzó el máximo de
    // fallos para esta IP dentro de la ventana. Con ip null (no se pudo resolver
    // la IP del request) no hay forma de aplicar el límite, así que no bloquea.
    public synchronized boolean registrarIntento(String ip) {
        if (ip == null) {
            return true;
        }

        List<LocalDateTime> fallos = fallosVigentes(ip);
        if (fallos.size() >= MAX_INTENTOS_FALLIDOS) {
            return false;
        }
        fallos.add(LocalDateTime.now());
        return true;
    }

    // Indica si la IP ya alcanzó el máximo de fallos dentro de la ventana, sin
    // registrar nada. AuthService lo consulta ANTES de validar las credenciales:
    // si solo se mirara al fallar, un atacante que acierta una contraseña después
    // de superar el límite igual entraría, y el bloqueo no frenaría el spraying.
    public synchronized boolean estaBloqueada(String ip) {
        if (ip == null) {
            return false;
        }
        return fallosVigentes(ip).size() >= MAX_INTENTOS_FALLIDOS;
    }

    // Devuelve la lista de fallos de la IP dentro de la ventana (creándola si no
    // existe), después de descartar los que ya quedaron fuera de ella.
    private List<LocalDateTime> fallosVigentes(String ip) {
        LocalDateTime limiteVentana = LocalDateTime.now().minusMinutes(VENTANA_MINUTOS);

        List<LocalDateTime> fallos = fallosPorIp.get(ip);
        if (fallos == null) {
            fallos = new ArrayList<>();
            fallosPorIp.put(ip, fallos);
        }

        Iterator<LocalDateTime> it = fallos.iterator();
        while (it.hasNext()) {
            if (it.next().isBefore(limiteVentana)) {
                it.remove();
            }
        }
        return fallos;
    }

    // Housekeeping: purga IPs sin fallos recientes para no acumular memoria
    // indefinidamente (mismo criterio que PasswordResetRateLimiter).
    @Scheduled(cron = "0 0 * * * *")
    public void limpiarExpirados() {
        LocalDateTime limiteVentana = LocalDateTime.now().minusMinutes(VENTANA_MINUTOS);
        int purgados = 0;
        Iterator<Map.Entry<String, List<LocalDateTime>>> it = fallosPorIp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<LocalDateTime>> entry = it.next();
            List<LocalDateTime> fallos = entry.getValue();

            Iterator<LocalDateTime> itFallos = fallos.iterator();
            while (itFallos.hasNext()) {
                if (itFallos.next().isBefore(limiteVentana)) {
                    itFallos.remove();
                }
            }

            if (fallos.isEmpty()) {
                it.remove();
                purgados++;
            }
        }
        if (purgados > 0) {
            log.info("[RATE-LIMIT] {} entrada(s) de rate-limit de login por IP purgadas", purgados);
        }
    }
}
