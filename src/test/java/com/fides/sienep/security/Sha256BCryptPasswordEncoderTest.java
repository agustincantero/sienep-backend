package com.fides.sienep.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class Sha256BCryptPasswordEncoderTest {

    private final Sha256BCryptPasswordEncoder encoder = new Sha256BCryptPasswordEncoder();

    @Test
    @DisplayName("Encode/matches - Contraseña normal")
    void matches_ContraseniaNormal() {
        String hash = encoder.encode("Admin123!");

        assertTrue(encoder.matches("Admin123!", hash));
        assertFalse(encoder.matches("Otra123!", hash));
    }

    @Test
    @DisplayName("Encode/matches - Contraseña de 100 caracteres (más de 72 bytes)")
    void matches_ContraseniaLarga() {
        String larga = "a".repeat(100);
        String hash = encoder.encode(larga);

        assertTrue(encoder.matches(larga, hash));
        // Con BCrypt plano los primeros 72 bytes bastarían; con el pre-hash no.
        assertFalse(encoder.matches("a".repeat(72), hash));
    }

    @Test
    @DisplayName("Encode/matches - Caracteres no ASCII")
    void matches_ContraseniaNoAscii() {
        String conTildes = "Contraseña-Ñandú-áéíóú";
        String hash = encoder.encode(conTildes);

        assertTrue(encoder.matches(conTildes, hash));
        assertFalse(encoder.matches("Contrasena-Nandu-aeiou", hash));
    }

    @Test
    @DisplayName("Matches - Hash previo al pre-hash (BCrypt plano) sigue validando")
    void matches_HashLegacyBCryptPlano() {
        String hashLegacy = new BCryptPasswordEncoder().encode("Admin123!");

        assertTrue(encoder.matches("Admin123!", hashLegacy));
        assertFalse(encoder.matches("Incorrecta1!", hashLegacy));
    }

    @Test
    @DisplayName("Matches - Contraseña de más de 72 bytes contra hash legacy no lanza excepción")
    void matches_ContraseniaLargaContraHashLegacy_NoLanza() {
        String hashLegacy = new BCryptPasswordEncoder().encode("Admin123!");

        assertFalse(encoder.matches("a".repeat(100), hashLegacy));
    }
}
