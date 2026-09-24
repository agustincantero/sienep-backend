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
        if (bcrypt.matches(sha256Hex(rawPassword), encodedPassword)) return true;
        // Hashes guardados antes de introducir el pre-hash: BCrypt directo sobre la
        // contraseña. Sin este fallback, todas las cuentas existentes dejan de poder
        // loguearse. Un hash BCrypt no indica cuál de los dos esquemas se usó, así que
        // se prueban ambos. Solo aplica hasta 72 bytes: las contraseñas más largas
        // nunca se pudieron guardar con el esquema anterior (y BCrypt las rechaza).
        // Cada cuenta pasa al esquema nuevo la próxima vez que cambia su contraseña.
        return rawPassword.toString().getBytes(StandardCharsets.UTF_8).length <= 72
                && bcrypt.matches(rawPassword, encodedPassword);
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
