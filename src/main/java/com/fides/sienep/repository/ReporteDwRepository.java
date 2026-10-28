package com.fides.sienep.repository;

import com.fides.sienep.dto.ReporteResponseDTO.EvolucionMensual;
import com.fides.sienep.dto.ReporteResponseDTO.IncidenciasDiaFranja;
import com.fides.sienep.dto.ReporteResponseDTO.IncidenciasInvolucradosMes;
import com.fides.sienep.dto.ReporteResponseDTO.IncidenciasLugarMes;
import com.fides.sienep.dto.ReporteResponseDTO.InstanciasCanalMes;
import com.fides.sienep.dto.ReporteResponseDTO.InstanciasCategoriaMes;
import com.fides.sienep.service.DwJdbc;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Lee las vistas v_* del Data Warehouse (IND-01 a IND-07), que ya vienen normalizadas y con porcentajes y promedios calculados; acá no se transforma nada. Las vistas están agregadas por mes, así que el período son meses completos: desde y hasta se pasan como el primer día del mes (la columna mes) y null significa sin límite.
@Repository
@RequiredArgsConstructor
public class ReporteDwRepository {

    private final DwJdbc dwJdbc;

    public List<EvolucionMensual> evolucionMensual(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT mes, anio_mes, instancias_agendadas, incidencias_ocurridas " +
                        "FROM datawarehouse.v_evolucion_mensual", "mes",
                desde, hasta,
                (rs, i) -> new EvolucionMensual(rs.getObject("mes", LocalDate.class), rs.getString("anio_mes"),
                        rs.getInt("instancias_agendadas"), rs.getInt("incidencias_ocurridas")));
    }

    public List<InstanciasCategoriaMes> instanciasPorCategoria(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT mes, anio_mes, categoria, instancias, porcentaje_del_mes " +
                        "FROM datawarehouse.v_instancias_categoria_mes", "mes, categoria",
                desde, hasta,
                (rs, i) -> new InstanciasCategoriaMes(rs.getObject("mes", LocalDate.class), rs.getString("anio_mes"),
                        rs.getString("categoria"), rs.getInt("instancias"), rs.getBigDecimal("porcentaje_del_mes")));
    }

    public List<InstanciasCanalMes> instanciasPorCanal(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT mes, anio_mes, canal, modalidad, instancias, porcentaje_del_mes " +
                        "FROM datawarehouse.v_instancias_canal_mes", "mes, canal",
                desde, hasta,
                (rs, i) -> new InstanciasCanalMes(rs.getObject("mes", LocalDate.class), rs.getString("anio_mes"),
                        rs.getString("canal"), rs.getString("modalidad"), rs.getInt("instancias"),
                        rs.getBigDecimal("porcentaje_del_mes")));
    }

    public List<IncidenciasLugarMes> incidenciasPorLugar(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT mes, anio_mes, tipo_espacio, lugar, incidencias " +
                        "FROM datawarehouse.v_incidencias_lugar_mes", "mes, tipo_espacio, lugar",
                desde, hasta,
                (rs, i) -> new IncidenciasLugarMes(rs.getObject("mes", LocalDate.class), rs.getString("anio_mes"),
                        rs.getString("tipo_espacio"), rs.getString("lugar"), rs.getInt("incidencias")));
    }

    public List<IncidenciasDiaFranja> incidenciasPorDiaFranja(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT mes, anio_mes, dia_semana_nro, dia_semana, franja_horaria, incidencias " +
                        "FROM datawarehouse.v_incidencias_dia_franja",
                "mes, dia_semana_nro, CASE franja_horaria WHEN 'Mañana' THEN 1 WHEN 'Mediodía' THEN 2 ELSE 3 END",
                desde, hasta,
                (rs, i) -> new IncidenciasDiaFranja(rs.getObject("mes", LocalDate.class), rs.getString("anio_mes"),
                        rs.getInt("dia_semana_nro"), rs.getString("dia_semana"), rs.getString("franja_horaria"),
                        rs.getInt("incidencias")));
    }

    public List<IncidenciasInvolucradosMes> incidenciasInvolucrados(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT mes, anio_mes, incidencias, total_involucrados, incidencias_con_mas_de_uno, " +
                        "promedio_involucrados, porcentaje_con_mas_de_uno " +
                        "FROM datawarehouse.v_incidencias_involucrados_mes", "mes",
                desde, hasta,
                (rs, i) -> new IncidenciasInvolucradosMes(rs.getObject("mes", LocalDate.class), rs.getString("anio_mes"),
                        rs.getInt("incidencias"), rs.getInt("total_involucrados"), rs.getInt("incidencias_con_mas_de_uno"),
                        rs.getBigDecimal("promedio_involucrados"), rs.getBigDecimal("porcentaje_con_mas_de_uno")));
    }

    // Fecha de la última carga correcta del DW; null si todavía no hubo ninguna
    public LocalDateTime ultimaActualizacion() {
        try {
            return dwJdbc.template().getJdbcOperations().query(
                    "SELECT fecha_actualizacion FROM datawarehouse.v_ultima_actualizacion",
                    rs -> rs.next() ? rs.getObject(1, LocalDateTime.class) : null);
        } catch (DataAccessException e) {
            throw DwJdbc.noDisponible("No se pudo consultar el Data Warehouse", e);
        }
    }

    private <T> List<T> consultar(String select, String orden, LocalDate desde, LocalDate hasta, RowMapper<T> mapper) {
        List<String> condiciones = new ArrayList<>();
        MapSqlParameterSource parametros = new MapSqlParameterSource();
        if (desde != null) {
            condiciones.add("mes >= :desde");
            parametros.addValue("desde", desde);
        }
        if (hasta != null) {
            condiciones.add("mes <= :hasta");
            parametros.addValue("hasta", hasta);
        }
        String sql = select + (condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones)) + " ORDER BY " + orden;

        try {
            return dwJdbc.template().query(sql, parametros, mapper);
        } catch (DataAccessException e) {
            throw DwJdbc.noDisponible("No se pudo consultar el Data Warehouse", e);
        }
    }
}
