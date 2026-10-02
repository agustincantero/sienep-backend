package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// RF06 — lo que conviene revisar antes de dar de baja a un estudiante: si hay algo, la baja pide confirmación.
@Schema(description = "Procesos abiertos de un estudiante, a revisar antes de darlo de baja")
public record ProcesosAbiertosDTO(
        @Schema(description = "Instancias activas agendadas a futuro", example = "2")
        long instanciasFuturas,
        @Schema(description = "Recordatorios activos, no cumplidos y desde hoy en adelante", example = "1")
        long recordatoriosPendientes) {

    // No viaja en el JSON (un record solo serializa sus componentes): el cliente lo deduce de los dos conteos.
    public boolean hayProcesosAbiertos() {
        return instanciasFuturas > 0 || recordatoriosPendientes > 0;
    }

    // Texto para el 409 de la baja sin confirmar, ej. "2 instancias agendadas y 1 recordatorio pendiente".
    public String describir() {
        StringBuilder sb = new StringBuilder();
        if (instanciasFuturas > 0) {
            sb.append(instanciasFuturas).append(instanciasFuturas == 1 ? " instancia agendada" : " instancias agendadas");
        }
        if (recordatoriosPendientes > 0) {
            if (!sb.isEmpty()) sb.append(" y ");
            sb.append(recordatoriosPendientes)
                    .append(recordatoriosPendientes == 1 ? " recordatorio pendiente" : " recordatorios pendientes");
        }
        return sb.toString();
    }
}
