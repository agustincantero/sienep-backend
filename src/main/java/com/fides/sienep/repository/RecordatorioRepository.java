package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Recordatorio;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecordatorioRepository extends JpaRepository<Recordatorio, Integer> {

    // JOIN FETCH de instancia/categoria/responsable: RecordatorioMapper accede
    // a las tres en cada fila, más frecuencia si no es null. Sin el fetch, listar N
    // recordatorios dispara SELECTs lazy adicionales por cada asociación y fila.
    // frecuencia es LEFT JOIN porque es opcional (id_frecuencia admite NULL); un
    // JOIN FETCH normal excluiría de los resultados los recordatorios sin frecuencia.
    @Query("SELECT r FROM Recordatorio r " +
            "JOIN FETCH r.instancia " +
            "JOIN FETCH r.categoria " +
            "JOIN FETCH r.responsable " +
            "LEFT JOIN FETCH r.frecuencia " +
            "WHERE r.estado = :estado")
    List<Recordatorio> findByEstado(@Param("estado") EstadoLogico estado);

    // Los 4 fetch son todos @ManyToOne/LEFT JOIN a-uno, no colecciones, así que
    // Hibernate pagina en SQL (LIMIT/OFFSET) sin caer al "in-memory paging".
    @Query("SELECT r FROM Recordatorio r " +
            "JOIN FETCH r.instancia " +
            "JOIN FETCH r.categoria " +
            "JOIN FETCH r.responsable " +
            "LEFT JOIN FETCH r.frecuencia " +
            "WHERE r.estado = :estado")
    Page<Recordatorio> findByEstado(@Param("estado") EstadoLogico estado, Pageable pageable);

    Optional<Recordatorio> findByCodRecordatorioAndEstado(int codRecordatorio, EstadoLogico estado);

    // Usado por FrecuenciaService.desactivar() para no dejar recordatorios
    // activos con una frecuencia inactiva (RecurrenceRuleBuilder seguiría
    // computando la recurrencia con datos de un catálogo dado de baja).
    boolean existsByFrecuencia_IdFrecuenciaAndEstado(int idFrecuencia, EstadoLogico estado);

    // RF27 — lock pesimista (SELECT ... FOR UPDATE) usado por
    // InstanciaComunService.crearDesdeRecordatorio(): sin esto, dos requests
    // concurrentes para el mismo recordatorio podrían leer "cumplido=false" antes
    // de que cualquiera de las dos lo marque, generando instancias de seguimiento
    // duplicadas. Con el lock, la segunda transacción queda bloqueada en esta
    // consulta hasta que la primera commitee, y al desbloquearse ve cumplido=true.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Recordatorio r WHERE r.codRecordatorio = :codRecordatorio AND r.estado = :estado")
    Optional<Recordatorio> findByCodRecordatorioAndEstadoParaActualizar(
            @Param("codRecordatorio") int codRecordatorio,
            @Param("estado") EstadoLogico estado);

    @Query("SELECT r FROM Recordatorio r " +
            "JOIN FETCH r.instancia " +
            "JOIN FETCH r.categoria " +
            "JOIN FETCH r.responsable " +
            "LEFT JOIN FETCH r.frecuencia " +
            "WHERE r.instancia.codInstancia = :codInstancia AND r.estado = :estado")
    List<Recordatorio> findByInstanciaAndEstado(@Param("codInstancia") int codInstancia,
                                                @Param("estado") EstadoLogico estado);

    @Query("SELECT r FROM Recordatorio r " +
            "JOIN FETCH r.instancia " +
            "JOIN FETCH r.categoria " +
            "JOIN FETCH r.responsable " +
            "LEFT JOIN FETCH r.frecuencia " +
            "WHERE r.instancia.codInstancia = :codInstancia AND r.estado = :estado")
    Page<Recordatorio> findByInstanciaAndEstado(@Param("codInstancia") int codInstancia,
                                                @Param("estado") EstadoLogico estado,
                                                Pageable pageable);

    // ── Reactivar ─────────────────────────────────────────────
    Optional<Recordatorio> findByCodRecordatorio(int codRecordatorio);

    // ── RF23: recordatorios próximos a vencer, aún no notificados ──
    // JOIN FETCH de responsable: RecordatorioNotificacionScheduler accede a
    // getResponsable().getEmail()/getNombre() por cada recordatorio de la lista.
    @Query("SELECT r FROM Recordatorio r JOIN FETCH r.responsable " +
            "WHERE r.estado = :estado AND r.notificadoProximidad = false " +
            "AND r.fechaHora BETWEEN :desde AND :hasta")
    List<Recordatorio> findByEstadoAndNotificadoProximidadFalseAndFechaHoraBetween(
            @Param("estado") EstadoLogico estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // RF06 — procesos abiertos antes de la baja de un estudiante: recordatorios activos, no cumplidos y desde hoy
    // en adelante, de instancias del estudiante. Por fecha y no por estado_operativo: ese lo recalcula el trigger
    // solo cuando se escribe el recordatorio, así que con los días queda desactualizado.
    @Query("SELECT COUNT(r) FROM Recordatorio r " +
            "WHERE r.instancia.estudiante.idUsuario = :idEstudiante AND r.estado = :estado " +
            "AND r.cumplido = false AND r.fechaHora >= :desde")
    long contarPendientesDelEstudiante(@Param("idEstudiante") int idEstudiante,
                                       @Param("estado") EstadoLogico estado,
                                       @Param("desde") LocalDateTime desde);

    // Devuelve cuántos recordatorios cambiaron de estado (ver proyecto.recalcular_estados_operativos).
    // @Transactional explícito: la función hace un UPDATE, y los métodos de consulta del
    // repositorio son readOnly por defecto.
    @Transactional
    @Query(value = "SELECT proyecto.recalcular_estados_operativos()", nativeQuery = true)
    int recalcularEstadosOperativos();
}
