package com.fides.sienep.utils;

public final class ReglasDeTexto {

    private ReglasDeTexto() {
    }

    // Debe coincidir con el CHECK chk_nombre_apellido_valido de proyecto.usuarios y con NOMBRE_REGEX del frontend
    public static final String NOMBRE_PERSONA_REGEX = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 '-]+$";

    public static final String NOMBRE_PERSONA_MSG = "Solo se permiten letras, números, espacios, apóstrofos y guiones";
}
