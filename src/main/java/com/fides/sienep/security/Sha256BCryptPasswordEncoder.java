package com.fides.sienep.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// BCrypt trunca (y en versiones recientes de Spring Security, rechaza con
// IllegalArgumentException) cualquier entrada de más de 72 bytes. Pre-hasheamos
// con SHA-256 para que BCrypt siempre reciba una entrada de longitud fija (64
// caracteres hex = 64 bytes ASCII), sin importar cuán larga sea la contraseña
// original ni bajar el máximo declarado en los DTOs (@Size(max = 100)).
public class Sha256BCryptPasswordEncoder implements PasswordEncoder {

    private final PasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(sha256Hex(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return bcrypt.matches(sha256Hex(rawPassword), encodedPassword);
    }

    private String sha256Hex(CharSequence rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
