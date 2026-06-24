package com.fides.sienep.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PasswordGeneratorService {

    private static final String MAYUSCULAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMEROS    = "0123456789";
    private static final String SIMBOLOS   = "!@#$%";
    private static final String TODOS      = MAYUSCULAS + MINUSCULAS + NUMEROS + SIMBOLOS;
    private static final int    LONGITUD   = 10;

    private final SecureRandom random = new SecureRandom();

    public String generar() {
        char[] contrasenia = new char[LONGITUD];

        // Garantizar al menos uno de cada tipo requerido
        contrasenia[0] = MAYUSCULAS.charAt(random.nextInt(MAYUSCULAS.length()));
        contrasenia[1] = NUMEROS.charAt(random.nextInt(NUMEROS.length()));
        contrasenia[2] = SIMBOLOS.charAt(random.nextInt(SIMBOLOS.length()));

        // Rellenar el resto
        for (int i = 3; i < LONGITUD; i++) {
            contrasenia[i] = TODOS.charAt(random.nextInt(TODOS.length()));
        }

        // Mezclar para que los caracteres garantizados no siempre estén al inicio
        for (int i = LONGITUD - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = contrasenia[i];
            contrasenia[i] = contrasenia[j];
            contrasenia[j] = temp;
        }

        return new String(contrasenia);
    }
}