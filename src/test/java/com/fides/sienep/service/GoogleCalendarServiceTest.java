package com.fides.sienep.service;

import com.fides.sienep.event.AccionCalendar;
import com.fides.sienep.event.CalendarInstanciaEvent;
import com.fides.sienep.event.CalendarRecordatorioEvent;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// IMPORTANTE — alcance de este test class:
// obtenerCliente() arma ServiceAccountCredentials.fromStream(...) (un factory
// estático de la librería de Google) y, con enabled=true, sincronizar() termina
// en un service.events().insert/update/delete(...).execute() real contra
// googleapis.com. Se evaluó mockear ServiceAccountCredentials con
// Mockito.mockStatic() (Mockito 5.23.0 trae el inline mock maker por defecto,
// a diferencia de cuando se escribió el test original de este service) — pero
// aunque eso resuelve la parte de credenciales, el Calendar.Builder/.events()/
// .insert()/.execute() se instancia con "new" adentro de obtenerCliente(), no
// inyectado, así que igual terminaría en una llamada de red real. Sin
// refactorizar a una interfaz wrapper (o sumar WireMock, que el proyecto no usa
// en ningún lado), el happy path con enabled=true no es testeable con Mockito
// puro — mismo gap documentado que en la versión anterior de este test class.
//
// Lo que SÍ es 100% testeable sin tocar el SDK ni la red:
//   1) el modo simulado (enabled=false) nunca intenta parsear credenciales ni
//      construir un cliente real — clave para RF22 en dev/test sin gastar cuota.
//   2) construirEvento(...) (package-private) arma el Event correcto: título,
//      descripción, attendees, fechas, rrule — es lógica pura, sin red.
class GoogleCalendarServiceTest {

    private GoogleCalendarService googleCalendarService;

    @BeforeEach
    void setUp() {
        googleCalendarService = new GoogleCalendarService();
        ReflectionTestUtils.setField(googleCalendarService, "calendarId", "calendario-test");
        ReflectionTestUtils.setField(googleCalendarService, "impersonateAccount", "sienep@utec.edu.uy");
        ReflectionTestUtils.setField(googleCalendarService, "applicationName", "SIENEP-test");
    }

    // ── Modo simulado (enabled=false) ───────────────────────────────────────

    @Test
    @DisplayName("procesar(Recordatorio) - Modo simulado, accion CREAR devuelve id SIMULADO-")
    void procesarRecordatorio_ModoSimulado_Crear_DevuelveIdSimulado() {
        ReflectionTestUtils.setField(googleCalendarService, "enabled", false);
        CalendarRecordatorioEvent evento = CalendarRecordatorioEvent.builder()
                .idRecordatorio(1)
                .idNegRecordatorio("REC-2026-000001")
                .accion(AccionCalendar.CREAR)
                .fechaHora(LocalDateTime.now().plusDays(1))
                .emailFuncionario("juan.garcia@utec.edu.uy")
                .nombreFuncionario("Juan")
                .build();

        String resultado = googleCalendarService.procesar(evento);

        assertNotNull(resultado);
        assertTrue(resultado.startsWith("SIMULADO-"));
    }

    @Test
    @DisplayName("procesar(Recordatorio) - Modo simulado, accion ELIMINAR devuelve null")
    void procesarRecordatorio_ModoSimulado_Eliminar_DevuelveNull() {
        ReflectionTestUtils.setField(googleCalendarService, "enabled", false);
        CalendarRecordatorioEvent evento = CalendarRecordatorioEvent.builder()
                .idRecordatorio(1)
                .idNegRecordatorio("REC-2026-000001")
                .oldEventId("evento-existente-id")
                .accion(AccionCalendar.ELIMINAR)
                .build();

        String resultado = googleCalendarService.procesar(evento);

        assertNull(resultado);
    }

    @Test
    @DisplayName("procesar(Instancia) - Modo simulado, accion ACTUALIZAR devuelve id SIMULADO-")
    void procesarInstancia_ModoSimulado_Actualizar_DevuelveIdSimulado() {
        ReflectionTestUtils.setField(googleCalendarService, "enabled", false);
        CalendarInstanciaEvent evento = CalendarInstanciaEvent.builder()
                .codInstancia(1)
                .idNegInstancia("INST-2026-000001")
                .oldEventId("evento-viejo-id")
                .accion(AccionCalendar.ACTUALIZAR)
                .fechaHora(LocalDateTime.now().plusDays(1))
                .emailFuncionario("juan.garcia@utec.edu.uy")
                .nombreFuncionario("Juan")
                .emailAlumno("ana@utec.edu.uy")
                .nombreAlumno("Ana")
                .build();

        String resultado = googleCalendarService.procesar(evento);

        assertNotNull(resultado);
        assertTrue(resultado.startsWith("SIMULADO-"));
    }

    // ── construirEvento(...) — sin red, sin SDK real más allá de los POJOs de modelo ──

    @Test
    @DisplayName("construirEvento - Usa el título y la descripción reales recibidos, con el ID en el título")
    void construirEvento_TituloYDescripcionReales() {
        Event evento = googleCalendarService.construirEvento(
                "INST-2026-000001", "Seguimiento con Juan", "Categoría: Académica — Canal: Presencial",
                LocalDateTime.now().plusDays(1), List.of(), null);

        assertEquals("Seguimiento con Juan — INST-2026-000001", evento.getSummary());
        assertEquals("Categoría: Académica — Canal: Presencial", evento.getDescription());
    }

    @Test
    @DisplayName("construirEvento - Con un solo attendee (recordatorio: solo funcionario)")
    void construirEvento_UnAttendee() {
        EventAttendee funcionario = new EventAttendee().setEmail("juan.garcia@utec.edu.uy").setDisplayName("Juan");

        Event evento = googleCalendarService.construirEvento(
                "REC-2026-000001", "titulo", "descripcion", LocalDateTime.now().plusDays(1), List.of(funcionario), null);

        assertEquals(1, evento.getAttendees().size());
        assertEquals("juan.garcia@utec.edu.uy", evento.getAttendees().get(0).getEmail());
    }

    @Test
    @DisplayName("construirEvento - Con dos attendees (instancia: funcionario + alumno)")
    void construirEvento_DosAttendees() {
        EventAttendee funcionario = new EventAttendee().setEmail("juan.garcia@utec.edu.uy").setDisplayName("Juan");
        EventAttendee alumno = new EventAttendee().setEmail("ana@utec.edu.uy").setDisplayName("Ana");

        Event evento = googleCalendarService.construirEvento(
                "INST-2026-000001", "titulo", "descripcion", LocalDateTime.now().plusDays(1), List.of(funcionario, alumno), null);

        assertEquals(2, evento.getAttendees().size());
        assertTrue(evento.getAttendees().stream().anyMatch(a -> a.getEmail().equals("ana@utec.edu.uy")));
    }

    @Test
    @DisplayName("construirEvento - Con rrule, setea la recurrencia; sin rrule, no")
    void construirEvento_Rrule() {
        Event conRrule = googleCalendarService.construirEvento(
                "REC-2026-000001", "titulo", "descripcion", LocalDateTime.now().plusDays(1), List.of(), "RRULE:FREQ=WEEKLY;BYDAY=MO");
        Event sinRrule = googleCalendarService.construirEvento(
                "REC-2026-000001", "titulo", "descripcion", LocalDateTime.now().plusDays(1), List.of(), null);

        assertEquals(List.of("RRULE:FREQ=WEEKLY;BYDAY=MO"), conRrule.getRecurrence());
        assertNull(sinRrule.getRecurrence());
    }

    @Test
    @DisplayName("construirEvento - Duración de 1 hora, fecha de fin = inicio + 1h")
    void construirEvento_DuracionUnaHora() {
        LocalDateTime inicio = LocalDateTime.of(2026, 3, 10, 14, 0);

        Event evento = googleCalendarService.construirEvento("INST-2026-000001", "titulo", "descripcion", inicio, List.of(), null);

        long inicioMs = evento.getStart().getDateTime().getValue();
        long finMs = evento.getEnd().getDateTime().getValue();
        assertEquals(60 * 60 * 1000L, finMs - inicioMs);
    }
}
