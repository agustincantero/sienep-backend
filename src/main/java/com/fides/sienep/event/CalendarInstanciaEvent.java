package com.fides.sienep.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// Publicado por InstanciaComunService dentro de su @Transactional; consumido
// por CalendarSyncListener recién después del commit (AFTER_COMMIT). Sin
// rrule: las instancias no tienen recurrencia (a diferencia de los
// recordatorios). El alumno SÍ es invitado acá — es la contracara de
// CalendarRecordatorioEvent, que es agenda interna sin el alumno.
@Getter
@Builder
public class CalendarInstanciaEvent {
    private final int codInstancia;
    private final String idNegInstancia;
    private final String oldEventId;
    private final AccionCalendar accion;
    private final String titulo;
    private final String descripcion;
    private final LocalDateTime fechaHora;
    private final String emailFuncionario;
    private final String nombreFuncionario;
    private final String emailAlumno;
    private final String nombreAlumno;
}
