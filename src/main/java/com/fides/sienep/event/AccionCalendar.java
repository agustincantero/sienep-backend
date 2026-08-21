package com.fides.sienep.event;

// Compartido por CalendarRecordatorioEvent/CalendarInstanciaEvent — le dice a
// GoogleCalendarService/CalendarSyncListener qué operación hacer contra el
// evento de Google Calendar, sin que el listener tenga que inferirlo de
// combinaciones de campos nulos.
public enum AccionCalendar {
    CREAR,
    ACTUALIZAR,
    ELIMINAR
}
