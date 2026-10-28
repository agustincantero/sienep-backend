package com.fides.sienep.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DwJdbcTest {

    private DwJdbc dwJdbc(String url, String usuario) {
        DwJdbc dwJdbc = new DwJdbc();
        ReflectionTestUtils.setField(dwJdbc, "url", url);
        ReflectionTestUtils.setField(dwJdbc, "usuario", usuario);
        ReflectionTestUtils.setField(dwJdbc, "contrasenia", "x");
        return dwJdbc;
    }

    @Test
    @DisplayName("Sin URL configurada responde 503 sin intentar conectarse")
    void template_SinUrl_Lanza503() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> dwJdbc("", "dw_lectura").template());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    @DisplayName("Sin usuario configurado responde 503")
    void template_SinUsuario_Lanza503() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> dwJdbc("jdbc:postgresql://localhost:5432/sienep_dw", "").template());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    @DisplayName("Con una URL inalcanzable responde 503 y no deja el pool a medias para el pedido siguiente")
    void template_BaseInalcanzable_Lanza503() {
        // Puerto 1 rechaza la conexión; el timeout corto evita que el test espere los 5 s reales
        DwJdbc dwJdbc = dwJdbc("jdbc:postgresql://localhost:1/sienep_dw", "dw_lectura");
        ReflectionTestUtils.setField(dwJdbc, "timeoutConexionMs", 250L);

        ResponseStatusException primero = assertThrows(ResponseStatusException.class, dwJdbc::template);
        ResponseStatusException segundo = assertThrows(ResponseStatusException.class, dwJdbc::template);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, primero.getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, segundo.getStatusCode());
    }
}
