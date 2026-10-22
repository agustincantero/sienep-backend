package com.fides.sienep.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

// Cifrado en reposo de los datos confidenciales (salud, motivo de derivación y
// comentarios confidenciales). A diferencia de la contraseña, que se hashea con
// BCrypt porque nunca hace falta volver a leerla, estos datos se muestran en la
// ficha: por eso se usa cifrado reversible (AES-256-GCM, que además detecta si
// el valor guardado fue alterado).
//
// Formato guardado: "enc:v1:" + Base64(IV de 12 bytes + texto cifrado + tag).
// El prefijo permite distinguir los valores cifrados de los que quedaron en
// claro de antes de este cambio (ver MigracionCifradoRunner), y la versión deja
// lugar para cambiar de algoritmo o de clave más adelante.
@Component
public class CifradorDatosConfidenciales {

    static final String PREFIJO = "enc:v1:";

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int LARGO_IV = 12;
    private static final int LARGO_TAG_BITS = 128;

    private final SecretKey clave;
    private final SecureRandom random = new SecureRandom();

    public CifradorDatosConfidenciales(@Value("${app.cifrado.clave}") String claveBase64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(claveBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("DATA_ENCRYPTION_KEY no es Base64 válido", e);
        }
        if (bytes.length != 32) {
            throw new IllegalStateException(
                    "DATA_ENCRYPTION_KEY tiene que ser de 32 bytes (AES-256) en Base64; tiene " + bytes.length);
        }
        this.clave = new SecretKeySpec(bytes, "AES");
    }

    public String cifrar(String texto) {
        if (texto == null) return null;
        try {
            byte[] iv = new byte[LARGO_IV];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(LARGO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] salida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return PREFIJO + Base64.getEncoder().encodeToString(salida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el dato confidencial", e);
        }
    }

    // Un valor sin el prefijo es un dato guardado en claro antes de este cambio:
    // se devuelve tal cual hasta que MigracionCifradoRunner lo cifre.
    public String descifrar(String valor) {
        if (valor == null || !estaCifrado(valor)) return valor;
        try {
            byte[] entrada = Base64.getDecoder().decode(valor.substring(PREFIJO.length()));
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(LARGO_TAG_BITS, entrada, 0, LARGO_IV));
            byte[] texto = cipher.doFinal(entrada, LARGO_IV, entrada.length - LARGO_IV);
            return new String(texto, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // Clave distinta a la que se usó al guardar, o valor alterado en la base.
            throw new IllegalStateException("No se pudo descifrar el dato confidencial", e);
        }
    }

    public boolean estaCifrado(String valor) {
        return valor != null && valor.startsWith(PREFIJO);
    }
}
