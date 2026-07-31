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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            LocalDate hasta,
            Pageable pageable) {

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

        // ── Resolver estudiantes según filtros ────────────────
        // pageable == null (exportación PDF): se trae la población completa que
        // matchea los filtros, sin paginar — el PDF es un documento autocontenido
        // y no tendría sentido que representara solo una página de resultados.
        // pageable != null (endpoint JSON): se pagina la población para no cargar
        // en memoria (ni serializar en la respuesta) miles de estudiantes cuando
        // no hay idCarrera/idGrupo/idEstudiante que acote el conjunto.
        List<Estudiante> estudiantes;
        Integer paginaActual = null;
        Integer tamanioPagina = null;
        Integer totalPaginas = null;
        Long totalEstudiantesFiltrados = null;

        if (pageable != null) {
            Page<Estudiante> pagina = resolverEstudiantesPaginado(idCarrera, idGrupo, idEstudiante, pageable);
            estudiantes = pagina.getContent();
            paginaActual = pagina.getNumber();
            tamanioPagina = pagina.getSize();
            totalPaginas = pagina.getTotalPages();
            totalEstudiantesFiltrados = pagina.getTotalElements();
        } else {
            estudiantes = resolverEstudiantes(idCarrera, idGrupo, idEstudiante);
        }

        // ── Advertencia por posibles duplicados ───────────────
        String advertencia = null;
        if (idCarrera != null || idGrupo != null) {
            advertencia = "El reporte filtra por grupo o carrera. Un estudiante inscripto en " +
                    "múltiples grupos puede aparecer en más de un resultado. " +
                    "Los totales reflejan la cantidad de registros, no de personas únicas.";
        }

        // ── Filtros aplicados ─────────────────────────────────
        String filtros = construirDescripcionFiltros(idCarrera, idGrupo, idEstudiante, fechaDesde, fechaHasta);

        // ── Traer instancias/incidencias/recordatorios de TODOS los estudiantes
        // en 3 queries (una por tipo), en vez de 3 por estudiante dentro del for
        // de abajo (N+1). Se agrupan en memoria por idEstudiante.
        List<Integer> idsEstudiantes = new ArrayList<>();
        for (Estudiante e : estudiantes) {
            idsEstudiantes.add(e.getIdUsuario());
        }

        Map<Integer, List<InstanciaComun>> instanciasPorEstudiante = new HashMap<>();
        Map<Integer, List<Incidencia>> incidenciasPorEstudiante = new HashMap<>();
        Map<Integer, List<Recordatorio>> recordatoriosPorEstudiante = new HashMap<>();

        if (!idsEstudiantes.isEmpty()) {
            for (InstanciaComun instancia : instanciaComunRepository.findByEstudiantesYPeriodo(
                    idsEstudiantes, EstadoLogico.ACTIVO, desdeDateTime, hastaDateTime)) {
                agregarAGrupo(instanciasPorEstudiante, instancia.getEstudiante().getIdUsuario(), instancia);
            }
            for (Incidencia incidencia : incidenciaRepository.findByEstudiantesYPeriodo(
                    idsEstudiantes, EstadoLogico.ACTIVO, desdeDateTime, hastaDateTime)) {
                agregarAGrupo(incidenciasPorEstudiante, incidencia.getEstudiante().getIdUsuario(), incidencia);
            }
            for (Recordatorio recordatorio : recordatorioRepository.findByEstudiantesYPeriodo(
                    idsEstudiantes, EstadoLogico.ACTIVO, desdeDateTime, hastaDateTime)) {
                agregarAGrupo(recordatoriosPorEstudiante,
                        recordatorio.getInstancia().getEstudiante().getIdUsuario(), recordatorio);
            }
        }

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

            List<InstanciaComun> instancias = instanciasPorEstudiante.getOrDefault(id, List.of());
            List<Incidencia> incidencias = incidenciasPorEstudiante.getOrDefault(id, List.of());
            List<Recordatorio> recordatorios = recordatoriosPorEstudiante.getOrDefault(id, List.of());

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
                .pagina(paginaActual)
                .tamanioPagina(tamanioPagina)
                .totalPaginas(totalPaginas)
                .totalEstudiantesFiltrados(totalEstudiantesFiltrados)
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
        return estudianteRepository.findByEstado(EstadoLogico.ACTIVO);
    }

    private Page<Estudiante> resolverEstudiantesPaginado(Integer idCarrera, Integer idGrupo,
                                                          Integer idEstudiante, Pageable pageable) {
        if (idEstudiante != null) {
            List<Estudiante> lista = new ArrayList<>();
            estudianteRepository.findById(idEstudiante).ifPresent(lista::add);
            return new PageImpl<>(lista, pageable, lista.size());
        }
        if (idGrupo != null) {
            return estudianteRepository.findByGrupoAndEstado(idGrupo, EstadoLogico.ACTIVO, pageable);
        }
        if (idCarrera != null) {
            return estudianteRepository.findByCarreraAndEstado(idCarrera, EstadoLogico.ACTIVO, pageable);
        }
        return estudianteRepository.findByEstado(EstadoLogico.ACTIVO, pageable);
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

    private <T> void agregarAGrupo(Map<Integer, List<T>> grupo, Integer idEstudiante, T elemento) {
        List<T> lista = grupo.get(idEstudiante);
        if (lista == null) {
            lista = new ArrayList<>();
            grupo.put(idEstudiante, lista);
        }
        lista.add(elemento);
    }
}