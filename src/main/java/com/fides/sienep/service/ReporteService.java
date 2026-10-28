package com.fides.sienep.service;

import com.fides.sienep.dto.ReporteResponseDTO;
import com.fides.sienep.dto.ReporteResponseDTO.EvolucionMensual;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.ReporteDwRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reporte con los 7 indicadores del Data Warehouse (IND-01 a IND-07), leídos de sus vistas v_*. La normalización y los
 * cálculos viven en el ETL y en las vistas, acá solo se arma la respuesta. Las vistas están agregadas por mes, así que
 * el período se redondea a meses completos. Es global (solo período) a propósito, porque carrera y grupo pertenecen
 * al estudiante y un estudiante puede cursar varias, así que no se pueden atribuir a una instancia sin duplicar conteos.
 */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final FuncionarioRepository funcionarioRepository;
    private final ReporteDwRepository reporteDwRepository;

    // Sin @Transactional a propósito: no escribe, y una transacción abierta retendría una conexión de la base operativa mientras se espera al DW.
    @PreAuthorize("hasAuthority('VER_REPORTES')")
    public ReporteResponseDTO generarReporte(LocalDate desde, LocalDate hasta) {

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha 'desde' no puede ser posterior a la fecha 'hasta'.");
        }

        // La columna mes de las vistas es el primer día de cada mes
        LocalDate mesDesde = desde != null ? desde.withDayOfMonth(1) : null;
        LocalDate mesHasta = hasta != null ? hasta.withDayOfMonth(1) : null;

        List<EvolucionMensual> evolucion = reporteDwRepository.evolucionMensual(mesDesde, mesHasta);

        int totalInstancias = 0;
        int totalIncidencias = 0;
        for (EvolucionMensual mes : evolucion) {
            totalInstancias += mes.getInstanciasAgendadas();
            totalIncidencias += mes.getIncidenciasOcurridas();
        }

        return ReporteResponseDTO.builder()
                .fechaGeneracion(LocalDateTime.now())
                .generadoPor(getNombreUsuarioAutenticado())
                .datosAl(reporteDwRepository.ultimaActualizacion())
                .desde(periodoDesde(desde, evolucion))
                .hasta(periodoHasta(hasta, evolucion))
                .totalInstancias(totalInstancias)
                .totalIncidencias(totalIncidencias)
                .evolucionMensual(evolucion)
                .instanciasPorCategoria(reporteDwRepository.instanciasPorCategoria(mesDesde, mesHasta))
                .instanciasPorCanal(reporteDwRepository.instanciasPorCanal(mesDesde, mesHasta))
                .incidenciasPorLugar(reporteDwRepository.incidenciasPorLugar(mesDesde, mesHasta))
                .incidenciasPorDiaFranja(reporteDwRepository.incidenciasPorDiaFranja(mesDesde, mesHasta))
                .incidenciasInvolucrados(reporteDwRepository.incidenciasInvolucrados(mesDesde, mesHasta))
                .build();
    }

    // ── Helpers privados ──────────────────────────────────────

    // Período efectivo en meses completos; sin parámetros se toma el rango de lo cargado, y es null si el DW no tiene datos
    private LocalDate periodoDesde(LocalDate desde, List<EvolucionMensual> evolucion) {
        if (desde != null) {
            return desde.withDayOfMonth(1);
        }
        return evolucion.isEmpty() ? null : evolucion.get(0).getMes();
    }

    private LocalDate periodoHasta(LocalDate hasta, List<EvolucionMensual> evolucion) {
        if (hasta != null) {
            return hasta.withDayOfMonth(hasta.lengthOfMonth());
        }
        if (evolucion.isEmpty()) {
            return null;
        }
        LocalDate ultimoMes = evolucion.get(evolucion.size() - 1).getMes();
        return ultimoMes.withDayOfMonth(ultimoMes.lengthOfMonth());
    }

    private String getNombreUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        int idUsuario = Integer.parseInt(auth.getName());

        Funcionario funcionario = funcionarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el funcionario autenticado"));

        // Mismo formato que el "Responsable" de las fichas del frontend (nombre y documento, para distinguir homónimos)
        return funcionario.getNombre() + " " + funcionario.getApellido() + " · " + funcionario.getDocumento();
    }
}
