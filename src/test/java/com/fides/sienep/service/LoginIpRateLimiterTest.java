package com.fides.sienep.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoginIpRateLimiterTest {

    private LoginIpRateLimiter rateLimiter;
    private static final String IP = "190.64.20.10";

    @BeforeEach
    void setUp() {
        rateLimiter = new LoginIpRateLimiter();
    }

    @Test
    @DisplayName("Registrar intento - Permite los primeros 20 fallos para la misma IP")
    void registrarIntento_PermiteHastaElLimite() {
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimiter.registrarIntento(IP), "Fallo número " + (i + 1) + " debería estar permitido");
        }
    }

    @Test
    @DisplayName("Registrar intento - Bloquea el fallo 21 dentro de la ventana")
    void registrarIntento_BloqueaAlSuperarElLimite() {
        for (int i = 0; i < 20; i++) {
            rateLimiter.registrarIntento(IP);
        }

        assertFalse(rateLimiter.registrarIntento(IP));
    }

    @Test
    @DisplayName("Registrar intento - El límite de una IP no afecta a otra distinta")
    void registrarIntento_NoConfundeIpsDistintas() {
        for (int i = 0; i < 20; i++) {
            rateLimiter.registrarIntento(IP);
        }

        assertTrue(rateLimiter.registrarIntento("200.10.5.1"));
    }

    @Test
    @DisplayName("Registrar intento - IP null no se puede limitar, siempre permite")
    void registrarIntento_IpNull_SiemprePermite() {
        for (int i = 0; i < 30; i++) {
            assertTrue(rateLimiter.registrarIntento(null));
        }
    }

    @Test
    @DisplayName("Registrar intento - Los fallos fuera de la ventana no cuentan para el límite")
    @SuppressWarnings("unchecked")
    void registrarIntento_IgnoraFallosFueraDeLaVentana() {
        Map<String, List<LocalDateTime>> fallosPorIp =
                (Map<String, List<LocalDateTime>>) ReflectionTestUtils.getField(rateLimiter, "fallosPorIp");

        List<LocalDateTime> viejos = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            viejos.add(LocalDateTime.now().minusMinutes(15));
        }
        fallosPorIp.put(IP, viejos);

        assertTrue(rateLimiter.registrarIntento(IP));
    }

    @Test
    @DisplayName("Limpiar expirados - No falla sin entradas y purga las que ya vencieron")
    @SuppressWarnings("unchecked")
    void limpiarExpirados_PurgaEntradasVencidas() {
        assertDoesNotThrow(() -> rateLimiter.limpiarExpirados());

        Map<String, List<LocalDateTime>> fallosPorIp =
                (Map<String, List<LocalDateTime>>) ReflectionTestUtils.getField(rateLimiter, "fallosPorIp");
        List<LocalDateTime> viejos = new ArrayList<>();
        viejos.add(LocalDateTime.now().minusMinutes(20));
        fallosPorIp.put("200.10.5.1", viejos);

        rateLimiter.limpiarExpirados();

        assertFalse(fallosPorIp.containsKey("200.10.5.1"));
    }
}
