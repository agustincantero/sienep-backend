package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Incidencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Integer> {

    List<Incidencia> findByEstado(EstadoLogico estado);

    Page<Incidencia> findByEstado(EstadoLogico estado, Pageable pageable);

    Optional<Incidencia> findByCodInstanciaAndEstado(int codInstancia, EstadoLogico estado);

    @Query("SELECT i FROM Incidencia i WHERE i.estudiante.idUsuario = :idEstudiante AND i.estado = :estado")
    List<Incidencia> findByEstudianteAndEstado(@Param("idEstudiante") int idEstudiante,
                                               @Param("estado") EstadoLogico estado);

    @Query("SELECT i FROM Incidencia i WHERE i.estudiante.idUsuario = :idEstudiante AND i.estado = :estado")
    Page<Incidencia> findByEstudianteAndEstado(@Param("idEstudiante") int idEstudiante,
                                               @Param("estado") EstadoLogico estado,
                                               Pageable pageable);

    @Query("SELECT i FROM Incidencia i WHERE i.funcionario.idUsuario = :idFuncionario AND i.estado = :estado")
    List<Incidencia> findByFuncionarioAndEstado(@Param("idFuncionario") int idFuncionario,
                                                @Param("estado") EstadoLogico estado);

    @Query("SELECT i FROM Incidencia i WHERE i.funcionario.idUsuario = :idFuncionario AND i.estado = :estado")
    Page<Incidencia> findByFuncionarioAndEstado(@Param("idFuncionario") int idFuncionario,
                                                @Param("estado") EstadoLogico estado,
                                                Pageable pageable);

    // ── Queries para Reportes ─────────────────────────────────
    @Query("SELECT i FROM Incidencia i " +
            "WHERE i.estudiante.idUsuario = :idEstudiante " +
            "AND i.estado = :estado " +
            "AND i.fechaHora BETWEEN :desde AND :hasta")
    List<Incidencia> findByEstudianteYPeriodo(
            @Param("idEstudiante") Integer idEstudiante,
            @Param("estado") EstadoLogico estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Versión "batch" de la anterior: evita llamar a findByEstudianteYPeriodo()
    // una vez por cada estudiante dentro de un for (N+1). Con IN (:idsEstudiantes)
    // se trae todo en una sola query y se agrupa en memoria por idEstudiante.
    @Query("SELECT i FROM Incidencia i " +
            "WHERE i.estudiante.idUsuario IN :idsEstudiantes " +
            "AND i.estado = :estado " +
            "AND i.fechaHora BETWEEN :desde AND :hasta")
    List<Incidencia> findByEstudiantesYPeriodo(
            @Param("idsEstudiantes") List<Integer> idsEstudiantes,
            @Param("estado") EstadoLogico estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}