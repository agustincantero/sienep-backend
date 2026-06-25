package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Recordatorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecordatorioRepository extends JpaRepository<Recordatorio, Integer> {

    List<Recordatorio> findByEstado(EstadoLogico estado);

    Optional<Recordatorio> findByCodRecordatorioAndEstado(int codRecordatorio, EstadoLogico estado);

    @Query("SELECT r FROM Recordatorio r WHERE r.instancia.codInstancia = :codInstancia AND r.estado = :estado")
    List<Recordatorio> findByInstanciaAndEstado(@Param("codInstancia") int codInstancia,
                                                @Param("estado") EstadoLogico estado);

    @Query("SELECT r FROM Recordatorio r WHERE r.responsable.idUsuario = :idResponsable AND r.estado = :estado")
    List<Recordatorio> findByResponsableAndEstado(@Param("idResponsable") int idResponsable,
                                                  @Param("estado") EstadoLogico estado);

    // ── Queries para Reportes ─────────────────────────────────
    @Query("SELECT r FROM Recordatorio r " +
            "WHERE r.instancia.estudiante.idUsuario = :idEstudiante " +
            "AND r.estado = :estado " +
            "AND r.fechaHora BETWEEN :desde AND :hasta")
    List<Recordatorio> findByEstudianteYPeriodo(
            @Param("idEstudiante") Integer idEstudiante,
            @Param("estado") EstadoLogico estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // ── Reactivar ─────────────────────────────────────────────
    Optional<Recordatorio> findByCodRecordatorio(int codRecordatorio);
}