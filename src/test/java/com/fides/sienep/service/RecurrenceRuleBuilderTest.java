package com.fides.sienep.utils;

import com.fides.sienep.model.DiaSemana;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Frecuencia;
import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.model.TipoFrecuencia;
import com.fides.sienep.model.TipoRecordatorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RecurrenceRuleBuilderTest {

    private RecurrenceRuleBuilder builder;
    private Recordatorio recordatorio;

    @BeforeEach
    void setUp() {
        builder = new RecurrenceRuleBuilder();

        recordatorio = new Recordatorio();
        recordatorio.setTitulo("Llamar a Ana");
        recordatorio.setTipo(TipoRecordatorio.LLAMADA);
        recordatorio.setFechaHora(LocalDateTime.now().plusDays(1));
    }

    private Frecuencia frecuencia(TipoFrecuencia tipo, Integer intDias, DiaSemana diaSemana) {
        Frecuencia f = new Frecuencia("Test", tipo, intDias, diaSemana);
        f.setEstado(EstadoLogico.ACTIVO);
        return f;
    }

    @Test
    @DisplayName("Sin frecuencia - devuelve null (recordatorio único, no recurrente)")
    void construir_SinFrecuencia_DevuelveNull() {
        recordatorio.setFrecuencia(null);

        assertNull(builder.construir(recordatorio));
    }

    @Test
    @DisplayName("DIARIA - FREQ=DAILY")
    void construir_Diaria() {
        recordatorio.setFrecuencia(frecuencia(TipoFrecuencia.DIARIA, null, null));

        assertEquals("RRULE:FREQ=DAILY", builder.construir(recordatorio));
    }

    @Test
    @DisplayName("SEMANAL con día - FREQ=WEEKLY;BYDAY=<abreviatura>")
    void construir_SemanalConDia() {
        recordatorio.setFrecuencia(frecuencia(TipoFrecuencia.SEMANAL, null, DiaSemana.LUNES));

        assertEquals("RRULE:FREQ=WEEKLY;BYDAY=MO", builder.construir(recordatorio));
    }

    @Test
    @DisplayName("SEMANAL sin día - FREQ=WEEKLY, sin BYDAY")
    void construir_SemanalSinDia() {
        recordatorio.setFrecuencia(frecuencia(TipoFrecuencia.SEMANAL, null, null));

        assertEquals("RRULE:FREQ=WEEKLY", builder.construir(recordatorio));
    }

    @Test
    @DisplayName("Todos los días de la semana mapean a su abreviatura RRULE correcta")
    void construir_TodosLosDiasDeLaSemana() {
        assertEquals("MO", diaEnRrule(DiaSemana.LUNES));
        assertEquals("TU", diaEnRrule(DiaSemana.MARTES));
        assertEquals("WE", diaEnRrule(DiaSemana.MIERCOLES));
        assertEquals("TH", diaEnRrule(DiaSemana.JUEVES));
        assertEquals("FR", diaEnRrule(DiaSemana.VIERNES));
        assertEquals("SA", diaEnRrule(DiaSemana.SABADO));
        assertEquals("SU", diaEnRrule(DiaSemana.DOMINGO));
    }

    private String diaEnRrule(DiaSemana dia) {
        Recordatorio r = new Recordatorio();
        r.setFrecuencia(frecuencia(TipoFrecuencia.SEMANAL, null, dia));
        String rrule = builder.construir(r);
        return rrule.substring(rrule.indexOf("BYDAY=") + 6);
    }

    @Test
    @DisplayName("MENSUAL - FREQ=MONTHLY")
    void construir_Mensual() {
        recordatorio.setFrecuencia(frecuencia(TipoFrecuencia.MENSUAL, null, null));

        assertEquals("RRULE:FREQ=MONTHLY", builder.construir(recordatorio));
    }

    @Test
    @DisplayName("PERSONALIZADA con intervalo - FREQ=DAILY;INTERVAL=<n>")
    void construir_PersonalizadaConIntervalo() {
        recordatorio.setFrecuencia(frecuencia(TipoFrecuencia.PERSONALIZADA, 3, null));

        assertEquals("RRULE:FREQ=DAILY;INTERVAL=3", builder.construir(recordatorio));
    }

    @Test
    @DisplayName("PERSONALIZADA sin intervalo - FREQ=DAILY, sin INTERVAL")
    void construir_PersonalizadaSinIntervalo() {
        recordatorio.setFrecuencia(frecuencia(TipoFrecuencia.PERSONALIZADA, null, null));

        assertEquals("RRULE:FREQ=DAILY", builder.construir(recordatorio));
    }

    @Test
    @DisplayName("PERSONALIZADA con intervalo 0 - se ignora (no es positivo)")
    void construir_PersonalizadaConIntervaloCero_SeIgnora() {
        recordatorio.setFrecuencia(frecuencia(TipoFrecuencia.PERSONALIZADA, 0, null));

        assertEquals("RRULE:FREQ=DAILY", builder.construir(recordatorio));
    }

    @Test
    @DisplayName("Con fechaFin - agrega UNTIL en formato YYYYMMDD")
    void construir_ConFechaFin_AgregaUntil() {
        recordatorio.setFrecuencia(frecuencia(TipoFrecuencia.DIARIA, null, null));
        recordatorio.setFechaFin(LocalDate.of(2026, 12, 31));

        assertEquals("RRULE:FREQ=DAILY;UNTIL=20261231", builder.construir(recordatorio));
    }

    @Test
    @DisplayName("Con nroOcurrencias - agrega COUNT")
    void construir_ConNroOcurrencias_AgregaCount() {
        recordatorio.setFrecuencia(frecuencia(TipoFrecuencia.DIARIA, null, null));
        recordatorio.setNroOcurrencias(5);

        assertEquals("RRULE:FREQ=DAILY;COUNT=5", builder.construir(recordatorio));
    }

    @Test
    @DisplayName("Con fechaFin Y nroOcurrencias - fechaFin tiene precedencia (UNTIL, no COUNT)")
    void construir_ConAmbosLimites_FechaFinTienePrecedencia() {
        recordatorio.setFrecuencia(frecuencia(TipoFrecuencia.DIARIA, null, null));
        recordatorio.setFechaFin(LocalDate.of(2026, 12, 31));
        recordatorio.setNroOcurrencias(5);

        String rrule = builder.construir(recordatorio);

        assertTrue(rrule.contains("UNTIL=20261231"));
        assertFalse(rrule.contains("COUNT="));
    }

    @Test
    @DisplayName("Con nroOcurrencias = 0 - se ignora (no es positivo), sin límite en el RRULE")
    void construir_ConNroOcurrenciasCero_SeIgnora() {
        recordatorio.setFrecuencia(frecuencia(TipoFrecuencia.DIARIA, null, null));
        recordatorio.setNroOcurrencias(0);

        assertEquals("RRULE:FREQ=DAILY", builder.construir(recordatorio));
    }
}