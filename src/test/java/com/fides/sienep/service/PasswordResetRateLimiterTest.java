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

class PasswordResetRateLimiterTest {

    private PasswordResetRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new PasswordResetRateLimiter();
    }

    @Test
    @DisplayName("Registrar intento - Permite los primeros 3 intentos para el mismo email")
    void registrarIntento_PermiteHastaElLimite() {
        assertTrue(rateLimiter.registrarIntento("juan.garcia@utec.edu.uy"));
        assertTrue(rateLimiter.registrarIntento("juan.garcia@utec.edu.uy"));
        assertTrue(rateLimiter.registrarIntento("juan.garcia@utec.edu.uy"));
    }

    @Test
    @DisplayName("Registrar intento - Bloquea el 4to intento dentro de la ventana")
    void registrarIntento_BloqueaAlSuperarElLimite() {
        rateLimiter.registrarIntento("juan.garcia@utec.edu.uy");
        rateLimiter.registrarIntento("juan.garcia@utec.edu.uy");
        rateLimiter.registrarIntento("juan.garcia@utec.edu.uy");

        assertFalse(rateLimiter.registrarIntento("juan.garcia@utec.edu.uy"));
    }

    @Test
    @DisplayName("Registrar intento - Se aplica igual para un email que no pertenece a ningún usuario")
    void registrarIntento_BloqueaIgualParaEmailInexistente() {
        rateLimiter.registrarIntento("inexistente@utec.edu.uy");
        rateLimiter.registrarIntento("inexistente@utec.edu.uy");
        rateLimiter.registrarIntento("inexistente@utec.edu.uy");

        assertFalse(rateLimiter.registrarIntento("inexistente@utec.edu.uy"));
    }

    @Test
    @DisplayName("Registrar intento - El límite de un email no afecta a otro distinto")
    void registrarIntento_NoConfundeEmailsDistintos() {
        rateLimiter.registrarIntento("juan.garcia@utec.edu.uy");
        rateLimiter.registrarIntento("juan.garcia@utec.edu.uy");
        rateLimiter.registrarIntento("juan.garcia@utec.edu.uy");

        assertTrue(rateLimiter.registrarIntento("otro.usuario@utec.edu.uy"));
    }

    @Test
    @DisplayName("Registrar intento - Normaliza mayúsculas/minúsculas como el mismo email")
    void registrarIntento_NormalizaMayusculas() {
        rateLimiter.registrarIntento("Juan.Garcia@utec.edu.uy");
        rateLimiter.registrarIntento("JUAN.GARCIA@UTEC.EDU.UY");
        rateLimiter.registrarIntento("juan.garcia@utec.edu.uy");

        assertFalse(rateLimiter.registrarIntento("juan.garcia@UTEC.edu.uy"));
    }

    @Test
    @DisplayName("Registrar intento - Los intentos fuera de la ventana no cuentan para el límite")
    @SuppressWarnings("unchecked")
    void registrarIntento_IgnoraIntentosFueraDeLaVentana() {
        Map<String, List<LocalDateTime>> intentosPorEmail =
                (Map<String, List<LocalDateTime>>) ReflectionTestUtils.getField(rateLimiter, "intentosPorEmail");

        List<LocalDateTime> viejos = new ArrayList<>();
        viejos.add(LocalDateTime.now().minusMinutes(20));
        viejos.add(LocalDateTime.now().minusMinutes(16));
        viejos.add(LocalDateTime.now().minusMinutes(30));
        intentosPorEmail.put("juan.garcia@utec.edu.uy", viejos);

        assertTrue(rateLimiter.registrarIntento("juan.garcia@utec.edu.uy"));
    }

    @Test
    @DisplayName("Limpiar expirados - No falla sin entradas y purga las que ya vencieron")
    @SuppressWarnings("unchecked")
    void limpiarExpirados_PurgaEntradasVencidas() {
        assertDoesNotThrow(() -> rateLimiter.limpiarExpirados());

        Map<String, List<LocalDateTime>> intentosPorEmail =
                (Map<String, List<LocalDateTime>>) ReflectionTestUtils.getField(rateLimiter, "intentosPorEmail");
        List<LocalDateTime> viejos = new ArrayList<>();
        viejos.add(LocalDateTime.now().minusMinutes(20));
        intentosPorEmail.put("viejo@utec.edu.uy", viejos);

        rateLimiter.limpiarExpirados();

        assertFalse(intentosPorEmail.containsKey("viejo@utec.edu.uy"));
    }
}
