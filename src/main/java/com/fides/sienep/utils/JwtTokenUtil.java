package com.fides.sienep.utils;

import com.fides.sienep.security.SienepUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtTokenUtil {

    private final SecretKey secretKey;

    // Vida útil del token. Mientras no exista blacklist ni refresh token, este valor
    // es el tiempo máximo que un token robado o de una sesión "cerrada" permanece válido.
    // Ver comentario en AuthService.logout() antes de modificar este valor.
    private static final long VALIDEZ_SEG = 5 * 60 * 60;

    public JwtTokenUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(int idUsuario, String rol) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", rol);
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(idUsuario))
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + VALIDEZ_SEG * 1000))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            if (!username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
                return false;
            }
            // Un cambio de contraseña no invalida por sí solo los tokens ya emitidos
            // (JWT es stateless, no hay revocación) — sin este chequeo, restablecer la
            // contraseña porque se sospecha de una sesión comprometida no cierra esa
            // sesión: el token viejo sigue funcionando hasta que expira solo.
            if (userDetails instanceof SienepUserDetails sienepUserDetails
                    && sienepUserDetails.getCredencialesVigentesDesde() != null
                    && extractIssuedAt(token).before(toDate(sienepUserDetails.getCredencialesVigentesDesde()))) {
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token no válido: {}", e.getMessage());
            return false;
        }
    }

    private Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    // El claim "iat" del JWT trunca a segundos enteros (así lo exige el spec), pero
    // credencialesVigentesDesde se guarda con LocalDateTime.now() (precisión de
    // milisegundos). Sin truncar acá, un token emitido genuinamente después de una
    // invalidación, pero dentro del mismo segundo de reloj, se rechazaría igual: su
    // iat redondeado hacia abajo quedaría "antes" de la marca con sub-segundos.
    private Date toDate(LocalDateTime fecha) {
        return Date.from(fecha.truncatedTo(ChronoUnit.SECONDS).atZone(ZoneId.systemDefault()).toInstant());
    }

    private Boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}