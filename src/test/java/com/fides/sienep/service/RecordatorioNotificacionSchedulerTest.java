package com.fides.sienep.service;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.repository.RecordatorioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordatorioNotificacionSchedulerTest {

    @Mock private RecordatorioRepository recordatorioRepository;
    @Mock private EmailService emailService;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private RecordatorioNotificacionScheduler scheduler;

    private Funcionario responsable;
    private Recordatorio recordatorio1;
    private Recordatorio recordatorio2;

    @BeforeEach
    void setUp() {
        responsable = new Funcionario();
        responsable.setIdUsuario(2);
        responsable.setNombre("Juan");
        responsable.setApellido("García");
        responsable.setEmail("juan.garcia@utec.edu.uy");
        responsable.setEstado(EstadoLogico.ACTIVO);

        recordatorio1 = new Recordatorio();
        recordatorio1.setCodRecordatorio(1);
        recordatorio1.setIdNegRecordatorio("REC-2026-000001");
        recordatorio1.setTitulo("Llamar a Ana");
        recordatorio1.setFechaHora(LocalDateTime.now().plusHours(5));
        recordatorio1.setResponsable(responsable);
        recordatorio1.setEstado(EstadoLogico.ACTIVO);
        recordatorio1.setNotificadoProximidad(false);

        recordatorio2 = new Recordatorio();
        recordatorio2.setCodRecordatorio(2);
        recordatorio2.setIdNegRecordatorio("REC-2026-000002");
        recordatorio2.setTitulo("Enviar informe");
        recordatorio2.setFechaHora(LocalDateTime.now().plusHours(10));
        recordatorio2.setResponsable(responsable);
        recordatorio2.setEstado(EstadoLogico.ACTIVO);
        recordatorio2.setNotificadoProximidad(false);
    }

    @Test
    @DisplayName("Con recordatorios próximos - envía email y marca notificadoProximidad")
    void notificarRecordatoriosProximos_ConRecordatorios_EnviaYMarca() {
        when(recordatorioRepository.findByEstadoAndNotificadoProximidadFalseAndFechaHoraBetween(
                eq(EstadoLogico.ACTIVO), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(recordatorio1, recordatorio2));
        when(emailService.enviarRecordatorioProximo(any(), any(), any(), any(), any())).thenReturn(true);

        scheduler.notificarRecordatoriosProximos();

        verify(emailService).enviarRecordatorioProximo(
                "juan.garcia@utec.edu.uy", "Juan", "REC-2026-000001", "Llamar a Ana", recordatorio1.getFechaHora());
        verify(emailService).enviarRecordatorioProximo(
                "juan.garcia@utec.edu.uy", "Juan", "REC-2026-000002", "Enviar informe", recordatorio2.getFechaHora());

        assertTrue(recordatorio1.isNotificadoProximidad());
        assertTrue(recordatorio2.isNotificadoProximidad());
        verify(recordatorioRepository).save(recordatorio1);
        verify(recordatorioRepository).save(recordatorio2);

        // RNF12 — cada envío queda auditado como enviado, sin autor porque lo dispara el sistema.
        verify(auditoriaService).registrarNotificacion("RECORDATORIOS", null, "RECORDATORIO_PROXIMO",
                "REC-2026-000001", responsable, true, null);
        verify(auditoriaService).registrarNotificacion("RECORDATORIOS", null, "RECORDATORIO_PROXIMO",
                "REC-2026-000002", responsable, true, null);
    }

    @Test
    @DisplayName("Sin recordatorios próximos - no envía nada ni guarda nada")
    void notificarRecordatoriosProximos_SinRecordatorios_NoHaceNada() {
        when(recordatorioRepository.findByEstadoAndNotificadoProximidadFalseAndFechaHoraBetween(
                eq(EstadoLogico.ACTIVO), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.notificarRecordatoriosProximos();

        verify(emailService, never()).enviarRecordatorioProximo(any(), any(), any(), any(), any());
        verify(recordatorioRepository, never()).save(any());
        verifyNoInteractions(auditoriaService);
    }

    @Test
    @DisplayName("Fallo al notificar uno no interrumpe la corrida del resto")
    void notificarRecordatoriosProximos_FalloEnUno_ContinuaConLosDemas() {
        when(recordatorioRepository.findByEstadoAndNotificadoProximidadFalseAndFechaHoraBetween(
                eq(EstadoLogico.ACTIVO), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(recordatorio1, recordatorio2));
        doThrow(new RuntimeException("Responsable con relación rota"))
                .when(emailService).enviarRecordatorioProximo(
                        any(), any(), eq("REC-2026-000001"), any(), any());

        assertDoesNotThrow(() -> scheduler.notificarRecordatoriosProximos());

        // El primero falló antes de poder marcarse -> no se guarda.
        verify(recordatorioRepository, never()).save(recordatorio1);
        // El segundo sí se procesa con normalidad.
        assertTrue(recordatorio2.isNotificadoProximidad());
        verify(recordatorioRepository).save(recordatorio2);

        // El fallo inesperado queda auditado como error, con el mensaje de la excepción como motivo.
        verify(auditoriaService).registrarNotificacion("RECORDATORIOS", null, "RECORDATORIO_PROXIMO",
                "REC-2026-000001", responsable, false, "Responsable con relación rota");
    }

    @Test
    @DisplayName("Correo rechazado - audita el error y marca igual notificadoProximidad")
    void notificarRecordatoriosProximos_CorreoRechazado_AuditaErrorYMarca() {
        when(recordatorioRepository.findByEstadoAndNotificadoProximidadFalseAndFechaHoraBetween(
                eq(EstadoLogico.ACTIVO), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(recordatorio1));
        when(emailService.enviarRecordatorioProximo(any(), any(), any(), any(), any())).thenReturn(false);

        scheduler.notificarRecordatoriosProximos();

        // No se reintenta cada hora un envío que falla: se marca, y el fallo queda en la auditoría.
        assertTrue(recordatorio1.isNotificadoProximidad());
        verify(recordatorioRepository).save(recordatorio1);
        verify(auditoriaService).registrarNotificacion("RECORDATORIOS", null, "RECORDATORIO_PROXIMO",
                "REC-2026-000001", responsable, false, "El servicio de correo no aceptó el envío");
    }

    @Test
    @DisplayName("Fallo al auditar no interrumpe la corrida del resto")
    void notificarRecordatoriosProximos_FalloAlAuditar_ContinuaConLosDemas() {
        when(recordatorioRepository.findByEstadoAndNotificadoProximidadFalseAndFechaHoraBetween(
                eq(EstadoLogico.ACTIVO), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(recordatorio1, recordatorio2));
        when(emailService.enviarRecordatorioProximo(any(), any(), any(), any(), any())).thenReturn(true);
        doThrow(new RuntimeException("BD caída"))
                .when(auditoriaService).registrarNotificacion(any(), any(), any(), eq("REC-2026-000001"), any(), anyBoolean(), any());

        assertDoesNotThrow(() -> scheduler.notificarRecordatoriosProximos());

        verify(emailService).enviarRecordatorioProximo(any(), any(), eq("REC-2026-000002"), any(), any());
        verify(auditoriaService).registrarNotificacion(any(), any(), any(), eq("REC-2026-000002"), any(), eq(true), any());
    }
}