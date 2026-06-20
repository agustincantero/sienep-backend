package com.fides.sienep.utils;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Genera el identificador de negocio de un recordatorio con el formato:
 * REC-{año}-{secuencia de 6 dígitos con ceros}
 * Ejemplo: REC-2026-000001
 *
 * Usa la SEQUENCE proyecto.seq_recordatorio definida en proyecto_schema.sql.
 * La columna id_neg_recordatorio es VARCHAR(16) — este formato ocupa exactamente 16 caracteres.
 */
@Component
@RequiredArgsConstructor
public class RecordatorioIdGenerator {

    private final EntityManager entityManager;

    public String generarIdNeg() {
        Long siguiente = (Long) entityManager
                .createNativeQuery("SELECT nextval('proyecto.seq_recordatorio')")
                .getSingleResult();

        int anio = LocalDate.now().getYear();

        return String.format("REC-%d-%06d", anio, siguiente);
    }
}