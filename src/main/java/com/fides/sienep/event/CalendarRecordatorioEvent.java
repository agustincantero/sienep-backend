package com.fides.sienep.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// Publicado por RecordatorioService dentro de su @Transactional; consumido por
// CalendarSyncListener recién después del commit (AFTER_COMMIT). Por eso lleva
// datos planos ya resueltos (emails, rrule) en vez de la entidad Recordatorio:
// para cuando el listener corre, la sesión de Hibernate que la cargó ya cerró.
//
// Los recordatorios son agenda interna del personal educativo — el alumno NO
// es invitado (a diferencia de CalendarInstanciaEvent).
@Getter
@Builder
public class CalendarRecordatorioEvent {
    private final int idRecordatorio;
    private final String idNegRecordatorio;
    private final String oldEventId;
    private final AccionCalendar accion;
    private final String titulo;
    private final String descripcion;
    private final LocalDateTime fechaHora;
    private final String rrule;
    private final String emailFuncionario;
    private final String nombreFuncionario;
}
