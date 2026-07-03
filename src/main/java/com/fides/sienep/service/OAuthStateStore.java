package com.fides.sienep.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Ahora: generarState() crea un token opaco y aleatorio (UUID de 122 bits de
// entropía) que el backend guarda internamente asociado al idUsuario que inició
// el flujo desde /google/init (endpoint autenticado con JWT). El callback de
// Google nunca ve ni puede inferir el idUsuario — solo puede presentar el token
// que Google le devolvió. consumir() lo valida, lo da de baja (single-use, evita
// replay) y recién ahí revela el idUsuario real al caller.
//
// Limitación conocida y aceptada para el alcance del proyecto: el store es en
// memoria del proceso. Si el backend corre en más de una instancia detrás de un
// balanceador, un init en la instancia A y un callback en la instancia B no
// encontrarían el token. Para un despliegue single-instance (como el túnel de
// Cloudflare que usa el equipo) esto no es un problema; si en el futuro se
// escala horizontalmente, este store debería migrar a Redis o a una tabla en
// PostgreSQL con el mismo contrato (generarState/consumir).
@Slf4j
@Component
public class OAuthStateStore {

    private static final long TTL_MINUTOS = 10;

    private final Map<String, StateEntry> estados = new ConcurrentHashMap<>();

    public String generarState(int idUsuario) {
        String state = UUID.randomUUID().toString();
        estados.put(state, new StateEntry(idUsuario, LocalDateTime.now().plusMinutes(TTL_MINUTOS)));
        return state;
    }

    // Single-use: el token se elimina apenas se lee, haya vencido o no.
    // Si alguien intenta reenviar el mismo callback dos veces (replay), la
    // segunda llamada ya no encuentra el token y falla.
    public Optional<Integer> consumir(String state) {
        StateEntry entry = estados.remove(state);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiraEn().isBefore(LocalDateTime.now())) {
            log.warn("[OAUTH] Intento de uso de state expirado para idUsuario={}", entry.idUsuario());
            return Optional.empty();
        }
        return Optional.of(entry.idUsuario());
    }

    // Housekeeping: purga tokens generados pero nunca usados (el funcionario inició
    // /google/init y nunca completó el flujo con Google). Sin esto, cada intento
    // abandonado quedaría ocupando memoria indefinidamente.
    @Scheduled(cron = "0 0 * * * *")
    public void limpiarExpirados() {
        LocalDateTime ahora = LocalDateTime.now();
        int purgados = 0;
        Iterator<Map.Entry<String, StateEntry>> it = estados.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expiraEn().isBefore(ahora)) {
                it.remove();
                purgados++;
            }
        }
        if (purgados > 0) {
            log.info("[OAUTH] {} token(s) de state expirados purgados", purgados);
        }
    }

    private record StateEntry(int idUsuario, LocalDateTime expiraEn) {}
}