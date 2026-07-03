package com.fides.sienep.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OAuthStateStoreTest {

    private OAuthStateStore store;

    @BeforeEach
    void setUp() {
        store = new OAuthStateStore();
    }

    @Test
    @DisplayName("Generar y consumir - devuelve el mismo idUsuario que se generó")
    void generarYConsumir_DevuelveMismoIdUsuario() {
        String state = store.generarState(42);

        Optional<Integer> resultado = store.consumir(state);

        assertTrue(resultado.isPresent());
        assertEquals(42, resultado.get());
    }

    @Test
    @DisplayName("Consumir un token inexistente devuelve vacío")
    void consumir_TokenInexistente_DevuelveVacio() {
        Optional<Integer> resultado = store.consumir("token-que-nunca-se-generó");

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Consumir el mismo token dos veces - la segunda vez falla (single-use)")
    void consumir_MismoTokenDosVeces_SegundaVezDevuelveVacio() {
        String state = store.generarState(42);

        Optional<Integer> primerUso = store.consumir(state);
        Optional<Integer> segundoUso = store.consumir(state);

        assertTrue(primerUso.isPresent());
        assertTrue(segundoUso.isEmpty());
    }

    @Test
    @DisplayName("Dos tokens generados para el mismo usuario son distintos entre sí")
    void generarState_LlamadasSucesivas_GeneranTokensDistintos() {
        String state1 = store.generarState(42);
        String state2 = store.generarState(42);

        assertNotEquals(state1, state2);
        // Ambos siguen siendo válidos y consumibles de forma independiente.
        assertTrue(store.consumir(state1).isPresent());
        assertTrue(store.consumir(state2).isPresent());
    }

    @Test
    @DisplayName("El state de un usuario no interfiere con el de otro")
    void consumir_NoConfundeUsuariosDistintos() {
        String stateUsuario1 = store.generarState(1);
        String stateUsuario2 = store.generarState(2);

        assertEquals(2, store.consumir(stateUsuario2).get());
        assertEquals(1, store.consumir(stateUsuario1).get());
    }
}