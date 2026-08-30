package com.fides.sienep.utils;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Genera el identificador de negocio de un recordatorio con el formato:
 * REC-{año}-{secuencia de 6 dígitos con ceros}
 * Ejemplo: REC-2026-000001
 *
 * Mismo mecanismo que InstanciaIdGenerator (ver su Javadoc para el detalle):
 * contador anual en proyecto.contador_id_negocio (tipo='RECORDATORIO'), con
 * UPSERT atómico en vez de una SEQUENCE global — el año resetea de verdad y
 * 6 dígitos alcanzan indefinidamente.
 */
@Component
@RequiredArgsConstructor
public class RecordatorioIdGenerator {

    private static final String TIPO = "RECORDATORIO";

    private final EntityManager entityManager;

    @Transactional
    public String generarIdNeg() {
        int anio = LocalDate.now().getYear();

        Number siguiente = (Number) entityManager.createNativeQuery(
                        "INSERT INTO proyecto.contador_id_negocio (tipo, anio, ultimo_valor) " +
                                "VALUES (:tipo, :anio, 1) " +
                                "ON CONFLICT (tipo, anio) DO UPDATE SET " +
                                "    ultimo_valor = proyecto.contador_id_negocio.ultimo_valor + 1 " +
                                "RETURNING ultimo_valor")
                .setParameter("tipo", TIPO)
                .setParameter("anio", anio)
                .getSingleResult();

        return String.format("REC-%d-%06d", anio, siguiente.intValue());
    }
}
