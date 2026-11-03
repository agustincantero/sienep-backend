package com.fides.sienep.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReglasDeTextoTest {

    private static final Pattern NOMBRE = Pattern.compile(ReglasDeTexto.NOMBRE_PERSONA_REGEX);

    @ParameterizedTest
    @ValueSource(strings = {"Pérez-Gómez", "D'Alessandro", "Françoise", "João", "Müller", "Ñandú", "ÇAGLAR", "SÃO", "María José", "Ana2"})
    @DisplayName("Nombre de persona - Acepta tildes, ñ, ü, ç, ã, apóstrofo y guion")
    void aceptaNombresValidos(String nombre) {
        assertEquals(true, NOMBRE.matcher(nombre).matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Łukasz", "Ștefan", "Dvořák", "Øystein", "Мария", "李", "O’Brien", "Ma.", "Ana_", "Ana@", "<b>"})
    @DisplayName("Nombre de persona - Rechaza otros caracteres especiales y alfabetos")
    void rechazaNombresInvalidos(String nombre) {
        assertEquals(false, NOMBRE.matcher(nombre).matches());
    }
}
