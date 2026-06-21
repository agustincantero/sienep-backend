package com.fides.sienep.service;

import com.fides.sienep.dto.ReporteEstudianteDTO;
import com.fides.sienep.dto.ReporteResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.EstadoRecordatorio;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Incidencia;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.GrupoRepository;
import com.fides.sienep.repository.IncidenciaRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.repository.RecordatorioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final EstudianteRepository estudianteRepository;
    private final GrupoRepository grupoRepository;
    private final InstanciaComunRepository instanciaComunRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final RecordatorioRepository recordatorioRepository;

    @PreAuthorize("hasAuthority('VER_REPORTES')")
    @Transactional(readOnly = true)
    public ReporteResponseDTO generarReporte(
            Integer idCarrera,
            Integer idGrupo,
            Integer idEstudiante,
            LocalDate desde,
            LocalDate hasta) {

        // ── Validación de fechas ──────────────────────────────
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha 'desde' no puede ser posterior a la fecha 'hasta'.");
        }

        // ── Normalizar período ────────────────────────────────
        LocalDate fechaDesde = desde != null ? desde : LocalDate.now().minusMonths(3);
        LocalDate fechaHasta = hasta != null ? hasta : LocalDate.now();
        LocalDateTime desdeDateTime = fechaDesde.atStartOfDay();
        LocalDateTime hastaDateTime = fechaHasta.atTime(23, 59, 59);

        // ── Resolver lista de estudiantes según filtros ───────
        List<Estudiante> estudiantes = resolverEstudiantes(idCarrera, idGrupo, idEstudiante);

        // ── Advertencia por posibles duplicados ───────────────
        String advertencia = null;
        if (idCarrera != null || idGrupo != null) {
            advertencia = "El reporte filtra por grupo o carrera. Un estudiante inscripto en " +
                    "múltiples grupos puede aparecer en más de un resultado. " +
                    "Los totales reflejan la cantidad de registros, no de personas únicas.";
        }

        // ── Filtros aplicados ─────────────────────────────────
        String filtros = construirDescripcionFiltros(idCarrera, idGrupo, idEstudiante, fechaDesde, fechaHasta);

        // ── Agregar métricas por estudiante ───────────────────
        List<ReporteEstudianteDTO> detalle = new ArrayList<>();
        int totalInstancias    = 0;
        int totalIncidencias   = 0;
        int totalRecordatorios = 0;
        int totalVencidos      = 0;
        int totalHoy           = 0;
        int totalFuturos       = 0;

        for (Estudiante e : estudiantes) {
            Integer id = e.getIdUsuario();

            List<InstanciaComun> instancias = instanciaComunRepository
                    .findByEstudianteYPeriodo(id, EstadoLogico.ACTIVO, desdeDateTime, hastaDateTime);

            List<Incidencia> incidencias = incidenciaRepository
                    .findByEstudianteYPeriodo(id, EstadoLogico.ACTIVO, desdeDateTime, hastaDateTime);

            // ── Una sola query para todos los recordatorios ───
            List<Recordatorio> recordatorios = recordatorioRepository
                    .findByEstudianteYPeriodo(id, EstadoLogico.ACTIVO, desdeDateTime, hastaDateTime);

            // ── Conteo en memoria por estado operativo ────────
            int vencidos = 0;
            int hoy      = 0;
            int futuros  = 0;
            for (Recordatorio r : recordatorios) {
                if (EstadoRecordatorio.VENCIDO.equals(r.getEstadoOperativo())) vencidos++;
                else if (EstadoRecordatorio.HOY.equals(r.getEstadoOperativo()))    hoy++;
                else if (EstadoRecordatorio.FUTURO.equals(r.getEstadoOperativo())) futuros++;
            }

            totalInstancias    += instancias.size();
            totalIncidencias   += incidencias.size();
            totalRecordatorios += recordatorios.size();
            totalVencidos      += vencidos;
            totalHoy           += hoy;
            totalFuturos       += futuros;

            detalle.add(ReporteEstudianteDTO.builder()
                    .idEstudiante(id)
                    .nombreCompleto(e.getNombre() + " " + e.getApellido())
                    .documento(e.getDocumento())
                    .cantidadInstancias(instancias.size())
                    .cantidadIncidencias(incidencias.size())
                    .cantidadRecordatorios(recordatorios.size())
                    .build());
        }

        return ReporteResponseDTO.builder()
                .fechaGeneracion(LocalDateTime.now())
                .generadoPor(getNombreUsuarioAutenticado())
                .filtrosAplicados(filtros)
                .advertencia(advertencia)
                .desde(fechaDesde)
                .hasta(fechaHasta)
                .totalEstudiantes(estudiantes.size())
                .totalInstancias(totalInstancias)
                .totalIncidencias(totalIncidencias)
                .totalRecordatorios(totalRecordatorios)
                .recordatoriosVencidos(totalVencidos)
                .recordatoriosHoy(totalHoy)
                .recordatoriosFuturos(totalFuturos)
                .detallePorEstudiante(detalle)
                .build();
    }

    // ── Helpers privados ──────────────────────────────────────

    private List<Estudiante> resolverEstudiantes(Integer idCarrera, Integer idGrupo, Integer idEstudiante) {
        if (idEstudiante != null) {
            List<Estudiante> lista = new ArrayList<>();
            estudianteRepository.findById(idEstudiante).ifPresent(lista::add);
            return lista;
        }
        if (idGrupo != null) {
            return estudianteRepository.findByGrupoAndEstadoList(idGrupo, EstadoLogico.ACTIVO);
        }
        if (idCarrera != null) {
            return estudianteRepository.findByCarreraAndEstadoList(idCarrera, EstadoLogico.ACTIVO);
        }
        return estudianteRepository.findAll();
    }

    private String construirDescripcionFiltros(Integer idCarrera, Integer idGrupo,
                                               Integer idEstudiante, LocalDate desde, LocalDate hasta) {
        List<String> partes = new ArrayList<>();
        if (idEstudiante != null) partes.add("Estudiante ID: " + idEstudiante);
        if (idGrupo != null)      partes.add("Grupo ID: " + idGrupo);
        if (idCarrera != null)    partes.add("Carrera ID: " + idCarrera);
        partes.add("Período: " + desde + " a " + hasta);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.size(); i++) {
            sb.append(partes.get(i));
            if (i < partes.size() - 1) sb.append(" | ");
        }
        return sb.toString();
    }

    private String getNombreUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }



}