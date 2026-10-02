package com.fides.sienep.utils;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.security.SienepUserDetails;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenUtilTest {

    private static final String SECRET = "clave-secreta-de-pruebas-con-al-menos-32-bytes-de-largo";

    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil(SECRET);
    private final SecretKey secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private String tokenConIssuedAt(Date issuedAt) {
        return Jwts.builder()
                .claims(Map.of("rol", "ESTUDIANTE"))
                .subject("1")
                .issuedAt(issuedAt)
                .expiration(new Date(System.currentTimeMillis() + 5 * 60 * 60 * 1000))
                .signWith(secretKey)
                .compact();
    }

    private SienepUserDetails userDetails(LocalDateTime credencialesVigentesDesde) {
        return new SienepUserDetails("1", "hash", Collections.emptyList(),
                credencialesVigentesDesde, EstadoLogico.ACTIVO);
    }

    @Test
    @DisplayName("validateToken - Token realmente emitido antes de credencialesVigentesDesde es inválido")
    void validateToken_EmitidoAntesDeInvalidacion_EsInvalido() {
        LocalDateTime credencialesVigentesDesde = LocalDateTime.now();
        Date issuedAt = Date.from(credencialesVigentesDesde.minusSeconds(10)
                .atZone(ZoneId.systemDefault()).toInstant());

        String token = tokenConIssuedAt(issuedAt);

        assertFalse(jwtTokenUtil.validateToken(token, userDetails(credencialesVigentesDesde)));
    }

    // El claim "iat" trunca a segundos enteros (NumericDate del spec de JWT). Sin truncar
    // credencialesVigentesDesde de la misma forma antes de comparar, un token emitido
    // genuinamente después de la invalidación, pero dentro del mismo segundo de reloj,
    // se rechazaba igual.
    @Test
    @DisplayName("validateToken - Token emitido dentro del mismo segundo que la invalidación, pero después, es válido")
    void validateToken_MismoSegundoTrasInvalidacion_EsValido() {
        Instant segundoBase = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime credencialesVigentesDesde =
                LocalDateTime.ofInstant(segundoBase.plusMillis(200), ZoneId.systemDefault());
        Date issuedAt = Date.from(segundoBase.plusMillis(800));

        String token = tokenConIssuedAt(issuedAt);

        assertTrue(jwtTokenUtil.validateToken(token, userDetails(credencialesVigentesDesde)));
    }

    @Test
    @DisplayName("validateToken - Sin credencialesVigentesDesde, el token es válido")
    void validateToken_SinCredencialesVigentesDesde_EsValido() {
        String token = tokenConIssuedAt(new Date());

        assertTrue(jwtTokenUtil.validateToken(token, userDetails(null)));
    }
}
