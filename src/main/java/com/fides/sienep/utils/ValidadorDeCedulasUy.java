package com.fides.sienep.utils;

/**
 * Valida cédulas uruguayas mediante el dígito verificador.
 * <p>
 * Acepta formatos con puntos o guiones, por ejemplo: "1.234.567-8".
 */
public final class ValidadorDeCedulasUy {

    private static final int[] PESOS = {2, 9, 8, 7, 6, 3, 4};

    private ValidadorDeCedulasUy() {
        // Evitar instancias
    }

    /**
     * Verifica si una cédula uruguaya es válida.
     *
     * @param cedula Cédula a validar
     * @return true si es válida, false si no
     * @throws IllegalArgumentException si la entrada es null
     */
    public static boolean esCedulaValida(String cedula) {
        if (cedula == null) {
            throw new IllegalArgumentException(
                    "La cédula no puede ser null."
            );
        }

        // Eliminar puntos, guiones y cualquier carácter no numérico
        String digits = cedula.replaceAll("\\D", "");

        // Una CI uruguaya válida debe tener entre 6 y 8 dígitos
        if (digits.length() < 6 || digits.length() > 8) {
            return false;
        }

        // Completar con ceros a la izquierda hasta 8 dígitos
        digits = String.format("%08d", Integer.parseInt(digits));

        int suma = 0;

        for (int i = 0; i < PESOS.length; i++) {
            int digito = Character.getNumericValue(
                    digits.charAt(i)
            );

            suma += digito * PESOS[i];
        }

        int resto = suma % 10;
        int digitoEsperado = (10 - resto) % 10;
        int digitoVerificador =
                Character.getNumericValue(digits.charAt(7));

        return digitoVerificador == digitoEsperado;
    }
}
