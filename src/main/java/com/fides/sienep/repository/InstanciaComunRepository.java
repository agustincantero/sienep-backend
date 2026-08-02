package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.InstanciaComun;
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
public interface InstanciaComunRepository extends JpaRepository<InstanciaComun, Integer> {

    // JOIN FETCH de estudiante/funcionario/categoria: InstanciaComunMapper
    // accede a los tres en cada fila (nombreEstudiante, nombreFuncionario,
    // nombreCategoria); sin el fetch, listar N instancias dispara 3N SELECTs
    // adicionales (uno lazy por asociación y por fila).
    @Query("SELECT ic FROM InstanciaComun ic " +
            "JOIN FETCH ic.estudiante " +
            "JOIN FETCH ic.funcionario " +
            "JOIN FETCH ic.categoria " +
            "WHERE ic.estado = :estado")
    List<InstanciaComun> findByEstado(@Param("estado") EstadoLogico estado);

    // Los 3 JOIN FETCH son todos @ManyToOne (a-uno), no colecciones, así que
    // Hibernate pagina en SQL (LIMIT/OFFSET) sin caer al "in-memory paging"
    // que sí ocurriría si alguno fuera una colección (@OneToMany/@ManyToMany).
    @Query("SELECT ic FROM InstanciaComun ic " +
            "JOIN FETCH ic.estudiante " +
            "JOIN FETCH ic.funcionario " +
            "JOIN FETCH ic.categoria " +
            "WHERE ic.estado = :estado")
    Page<InstanciaComun> findByEstado(@Param("estado") EstadoLogico estado, Pageable pageable);

    Optional<InstanciaComun> findByCodInstanciaAndEstado(int codInstancia, EstadoLogico estado);

    @Query("SELECT ic FROM InstanciaComun ic " +
            "JOIN FETCH ic.estudiante " +
            "JOIN FETCH ic.funcionario " +
            "JOIN FETCH ic.categoria " +
            "WHERE ic.estudiante.idUsuario = :idEstudiante AND ic.estado = :estado")
    Page<InstanciaComun> findByEstudianteAndEstado(@Param("idEstudiante") int idEstudiante,
                                                   @Param("estado") EstadoLogico estado,
                                                   Pageable pageable);

    @Query("SELECT ic FROM InstanciaComun ic " +
            "JOIN FETCH ic.estudiante " +
            "JOIN FETCH ic.funcionario " +
            "JOIN FETCH ic.categoria " +
            "WHERE ic.funcionario.idUsuario = :idFuncionario AND ic.estado = :estado")
    List<InstanciaComun> findByFuncionarioAndEstado(@Param("idFuncionario") int idFuncionario,
                                                    @Param("estado") EstadoLogico estado);

    @Query("SELECT ic FROM InstanciaComun ic " +
            "JOIN FETCH ic.estudiante " +
            "JOIN FETCH ic.funcionario " +
            "JOIN FETCH ic.categoria " +
            "WHERE ic.funcionario.idUsuario = :idFuncionario AND ic.estado = :estado")
    Page<InstanciaComun> findByFuncionarioAndEstado(@Param("idFuncionario") int idFuncionario,
                                                    @Param("estado") EstadoLogico estado,
                                                    Pageable pageable);

    @Query("SELECT ic FROM InstanciaComun ic " +
            "JOIN FETCH ic.estudiante " +
            "JOIN FETCH ic.funcionario " +
            "JOIN FETCH ic.categoria " +
            "WHERE ic.categoria.idCategoria = :idCategoria AND ic.estado = :estado")
    List<InstanciaComun> findByCategoriaAndEstado(@Param("idCategoria") int idCategoria,
                                                  @Param("estado") EstadoLogico estado);

    @Query("SELECT ic FROM InstanciaComun ic " +
            "JOIN FETCH ic.estudiante " +
            "JOIN FETCH ic.funcionario " +
            "JOIN FETCH ic.categoria " +
            "WHERE ic.categoria.idCategoria = :idCategoria AND ic.estado = :estado")
    Page<InstanciaComun> findByCategoriaAndEstado(@Param("idCategoria") int idCategoria,
                                                  @Param("estado") EstadoLogico estado,
                                                  Pageable pageable);

    // ── Queries para Reportes ─────────────────────────────────
    @Query("SELECT ic FROM InstanciaComun ic " +
            "WHERE ic.estudiante.idUsuario = :idEstudiante " +
            "AND ic.estado = :estado " +
            "AND ic.fechaHora BETWEEN :desde AND :hasta")
    List<InstanciaComun> findByEstudianteYPeriodo(
            @Param("idEstudiante") Integer idEstudiante,
            @Param("estado") EstadoLogico estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    // Versión "batch" de la anterior: evita llamar a findByEstudianteYPeriodo()
    // una vez por cada estudiante dentro de un for (N+1). Con IN (:idsEstudiantes)
    // se trae todo en una sola query y se agrupa en memoria por idEstudiante.
    @Query("SELECT ic FROM InstanciaComun ic " +
            "WHERE ic.estudiante.idUsuario IN :idsEstudiantes " +
            "AND ic.estado = :estado " +
            "AND ic.fechaHora BETWEEN :desde AND :hasta")
    List<InstanciaComun> findByEstudiantesYPeriodo(
            @Param("idsEstudiantes") List<Integer> idsEstudiantes,
            @Param("estado") EstadoLogico estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}