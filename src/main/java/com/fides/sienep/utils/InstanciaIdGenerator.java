package com.fides.sienep.utils;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Genera el identificador de negocio de una instancia con el formato:
 * INST-{año}-{secuencia de 6 dígitos con ceros}
 * Ejemplo: INST-2026-000001
 *
 * Usa la SEQUENCE proyecto.seq_instancia definida en proyecto_schema.sql.
 * La columna id_neg_instancia es VARCHAR(16) — este formato ocupa exactamente 16 caracteres.
 */
@Component
@RequiredArgsConstructor
public class InstanciaIdGenerator {

    private final EntityManager entityManager;

    public String generarIdNeg() {
        Long siguiente = (Long) entityManager
                .createNativeQuery("SELECT nextval('proyecto.seq_instancia')")
                .getSingleResult();

        int anio = LocalDate.now().getYear();

        return String.format("INST-%d-%06d", anio, siguiente);
    }
}