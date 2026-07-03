package com.fides.sienep.utils;

import com.fides.sienep.model.Frecuencia;
import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.model.TipoFrecuencia;
import org.springframework.stereotype.Component;

// Extraído de GoogleCalendarService para que la lógica de mapeo Frecuencia → RRULE
// (iCalendar, usado por Google Calendar) sea testeable sin depender del SDK de Google
// ni de la red. Es lógica pura: mismo input, mismo output, sin I/O.
//
// Mapeo:
//   DIARIA        → FREQ=DAILY
//   SEMANAL       → FREQ=WEEKLY;BYDAY=<DiaSemana>
//   MENSUAL       → FREQ=MONTHLY
//   PERSONALIZADA → FREQ=DAILY;INTERVAL=<intDias>
//
// Si el recordatorio tiene fechaFin → UNTIL=<fecha> (tiene precedencia)
// Si tiene nroOcurrencias           → COUNT=<n>
// Si no tiene ninguno de los dos    → sin límite (poco probable por validaciones del DTO)
@Component
public class RecurrenceRuleBuilder {

    public String construir(Recordatorio recordatorio) {
        Frecuencia frecuencia = recordatorio.getFrecuencia();
        if (frecuencia == null) {
            return null;
        }

        StringBuilder rrule = new StringBuilder("RRULE:FREQ=");

        if (frecuencia.getTipo() == TipoFrecuencia.DIARIA) {
            rrule.append("DAILY");

        } else if (frecuencia.getTipo() == TipoFrecuencia.SEMANAL) {
            rrule.append("WEEKLY");
            if (frecuencia.getDiaSemana() != null) {
                String byday = mapearDiaSemana(frecuencia.getDiaSemana().name());
                if (byday != null) {
                    rrule.append(";BYDAY=").append(byday);
                }
            }

        } else if (frecuencia.getTipo() == TipoFrecuencia.MENSUAL) {
            rrule.append("MONTHLY");

        } else if (frecuencia.getTipo() == TipoFrecuencia.PERSONALIZADA) {
            rrule.append("DAILY");
            if (frecuencia.getIntDias() != null && frecuencia.getIntDias() > 0) {
                rrule.append(";INTERVAL=").append(frecuencia.getIntDias());
            }
        } else {
            return null;
        }

        if (recordatorio.getFechaFin() != null) {
            String hasta = recordatorio.getFechaFin().toString().replace("-", "");
            rrule.append(";UNTIL=").append(hasta);
        } else if (recordatorio.getNroOcurrencias() != null && recordatorio.getNroOcurrencias() > 0) {
            rrule.append(";COUNT=").append(recordatorio.getNroOcurrencias());
        }

        return rrule.toString();
    }

    private String mapearDiaSemana(String diaSemana) {
        switch (diaSemana) {
            case "LUNES":     return "MO";
            case "MARTES":    return "TU";
            case "MIERCOLES": return "WE";
            case "JUEVES":    return "TH";
            case "VIERNES":   return "FR";
            case "SABADO":    return "SA";
            case "DOMINGO":   return "SU";
            default:          return null;
        }
    }
}