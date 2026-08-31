package com.fides.sienep.utils;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Genera el identificador de negocio de una instancia con el formato:
 * INST-{año}-{secuencia de 6 dígitos con ceros}
 * Ejemplo: INST-2026-000001
 *
 * A diferencia de una SEQUENCE global de Postgres, el contador vive en
 * proyecto.contador_id_negocio particionado por (tipo, año): cada año nuevo
 * arranca en 1, así que el formato "anual" que sugiere el id es real (no
 * cosmético) y 6 dígitos alcanzan indefinidamente — no hay overflow salvo que
 * un solo año acumule más de 999.999 instancias.
 *
 * El incremento usa un UPSERT atómico (INSERT ... ON CONFLICT DO UPDATE ...
 * RETURNING): Postgres serializa las filas en conflicto con un lock de fila, así
 * que dos requests concurrentes en el mismo año nunca reciben el mismo valor, sin
 * depender de un cron que resetee algo a medianoche del 1/1 (ese enfoque sería
 * frágil: si el proceso está caído justo en ese momento, o corre más de una
 * instancia de la app, el reset se puede perder o duplicar).
 *
 * Nota: al ser transaccional (join de la transacción del caller, ver crear() en
 * los services), si esa transacción hace rollback el incremento también se
 * deshace — a diferencia de una SEQUENCE, acá no quedan huecos permanentes por
 * transacciones fallidas.
 */
@Component
@RequiredArgsConstructor
public class InstanciaIdGenerator {

    private static final String TIPO = "INSTANCIA";

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

        return String.format("INST-%d-%06d", anio, siguiente.intValue());
    }
}
