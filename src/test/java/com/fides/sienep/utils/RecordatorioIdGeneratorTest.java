package com.fides.sienep.utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordatorioIdGeneratorTest {

    @Mock private EntityManager entityManager;
    @Mock private Query query;

    @InjectMocks
    private RecordatorioIdGenerator generator;

    private void mockContador(int ultimoValor) {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(ultimoValor);
    }

    @Test
    void generarIdNeg_FormateaConSeisDigitosYAnioActual() {
        mockContador(1);

        String id = generator.generarIdNeg();

        int anioActual = LocalDate.now().getYear();
        assertEquals("REC-" + anioActual + "-000001", id);
    }

    // H21 — a diferencia de la vieja SEQUENCE global, el contador resetea por
    // año: un ultimo_valor bajo (ej. 1) sigue siendo válido sin importar cuántos
    // recordatorios se hayan creado en años anteriores.
    @Test
    void generarIdNeg_UsaElContadorDelAnioActual_SinImportarAnosPrevios() {
        mockContador(42);

        String id = generator.generarIdNeg();

        int anioActual = LocalDate.now().getYear();
        assertEquals("REC-" + anioActual + "-000042", id);
    }

    @Test
    void generarIdNeg_ConsultaElContadorConElTipoYElAnioCorrectos() {
        mockContador(1);
        int anioActual = LocalDate.now().getYear();

        generator.generarIdNeg();

        org.mockito.Mockito.verify(query).setParameter("tipo", "RECORDATORIO");
        org.mockito.Mockito.verify(query).setParameter("anio", anioActual);
    }
}
