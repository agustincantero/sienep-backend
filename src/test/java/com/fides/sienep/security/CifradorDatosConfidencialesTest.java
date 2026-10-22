package com.fides.sienep.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CifradorDatosConfidencialesTest {

    private static final String CLAVE = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTRA_CLAVE = Base64.getEncoder().encodeToString("x".repeat(32).getBytes());

    private final CifradorDatosConfidenciales cifrador = new CifradorDatosConfidenciales(CLAVE);

    @Test
    @DisplayName("Cifrar/descifrar - Devuelve el texto original, con tildes y saltos de línea")
    void cifrarDescifrar_TextoOriginal() {
        String texto = "Diagnóstico: TDAH.\nTratamiento con psicopedagoga — Ñandú";
        String cifrado = cifrador.cifrar(texto);

        assertTrue(cifrado.startsWith("enc:v1:"));
        assertFalse(cifrado.contains("TDAH"));
        assertEquals(texto, cifrador.descifrar(cifrado));
    }

    @Test
    @DisplayName("Cifrar - El mismo texto da un resultado distinto cada vez (IV aleatorio)")
    void cifrar_IvAleatorio() {
        assertNotEquals(cifrador.cifrar("ASSE"), cifrador.cifrar("ASSE"));
    }

    @Test
    @DisplayName("Null y vacío se mantienen")
    void nullYVacio() {
        assertNull(cifrador.cifrar(null));
        assertNull(cifrador.descifrar(null));
        assertEquals("", cifrador.descifrar(cifrador.cifrar("")));
    }

    @Test
    @DisplayName("Descifrar - Un valor viejo guardado en claro se devuelve tal cual")
    void descifrar_ValorEnClaro() {
        assertEquals("ASSE", cifrador.descifrar("ASSE"));
        assertFalse(cifrador.estaCifrado("ASSE"));
    }

    @Test
    @DisplayName("Cifrar - Un texto que ya empieza con el prefijo también se cifra")
    void cifrar_TextoConPrefijo() {
        String texto = "enc:v1:esto lo escribió una persona";
        assertEquals(texto, cifrador.descifrar(cifrador.cifrar(texto)));
    }

    @Test
    @DisplayName("Descifrar - Falla con otra clave o si el valor fue alterado")
    void descifrar_ClaveIncorrectaOAlterado() {
        String cifrado = cifrador.cifrar("Información de salud");

        CifradorDatosConfidenciales otro = new CifradorDatosConfidenciales(OTRA_CLAVE);
        assertThrows(IllegalStateException.class, () -> otro.descifrar(cifrado));

        char ultimo = cifrado.charAt(cifrado.length() - 2);
        String alterado = cifrado.substring(0, cifrado.length() - 2) + (ultimo == 'A' ? 'B' : 'A') + cifrado.charAt(cifrado.length() - 1);
        assertThrows(IllegalStateException.class, () -> cifrador.descifrar(alterado));
    }

    @Test
    @DisplayName("Constructor - Rechaza claves que no son de 32 bytes en Base64")
    void constructor_ClaveInvalida() {
        assertThrows(IllegalStateException.class, () -> new CifradorDatosConfidenciales("no-es-base64!"));
        String corta = Base64.getEncoder().encodeToString(new byte[16]);
        assertThrows(IllegalStateException.class, () -> new CifradorDatosConfidenciales(corta));
    }
}
