package com.fides.sienep.repository;

import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
// JpaSpecificationExecutor: el listado/búsqueda de estudiantes (EstudianteService.listar) combina filtros
// opcionales con una Specification (ver EstudianteSpecs).
public interface EstudianteRepository extends JpaRepository<Estudiante, Integer>, JpaSpecificationExecutor<Estudiante> {

    Optional<Estudiante> findByEmail(String email);

    // Para GET /auth/me: trae rol + permisos en una sola query (evita N+1 y no
    // depende de open-in-view). LEFT en rolPermisos/permiso: un rol sin permisos
    // igual resuelve el estudiante (con permisos = []). Mismo criterio que FuncionarioRepository.
    @Query("SELECT e FROM Estudiante e " +
            "JOIN FETCH e.rol r " +
            "LEFT JOIN FETCH r.rolPermisos rp " +
            "LEFT JOIN FETCH rp.permiso " +
            "WHERE e.idUsuario = :id")
    Optional<Estudiante> findByIdConRolYPermisos(@Param("id") int id);

    // Listado por estado
    Page<Estudiante> findByEstado(EstadoLogico estado, Pageable pageable);

    // Filtro por grupo con estado — usado por ReporteService (el listado de estudiantes usa Specification).
    @Query("SELECT e FROM Estudiante e JOIN e.grupos g WHERE g.idGrupo = :idGrupo AND e.estado = :estado")
    Page<Estudiante> findByGrupoAndEstado(@Param("idGrupo") Integer idGrupo,
                                          @Param("estado") EstadoLogico estado,
                                          Pageable pageable);

    // Filtro por carrera con estado — usado por ReporteService.
    @Query("""
            SELECT DISTINCT e FROM Estudiante e
            JOIN e.grupos g JOIN g.carrera c
            WHERE c.idCarrera = :idCarrera AND e.estado = :estado
            """)
    Page<Estudiante> findByCarreraAndEstado(@Param("idCarrera") Integer idCarrera,
                                            @Param("estado") EstadoLogico estado,
                                            Pageable pageable);

    // Validaciones de unicidad
    boolean existsByDocumento(String documento);
    boolean existsByEmail(String email);

    // Para editar(): excluye al propio estudiante, si no cualquier edición que no
    // toque el email fallaría al "chocar" contra su propio registro.
    boolean existsByEmailAndIdUsuarioNot(String email, int idUsuario);


    // ── Queries para Reportes — sin paginación ────────────────
    // Usado por ReporteService solo para la exportación PDF (que trae la
    // población completa que matchea los filtros, ya que el documento debe ser
    // autocontenido). El endpoint JSON de reportes usa las sobrecargas paginadas
    // de arriba (findByEstado/findByGrupoAndEstado/findByCarreraAndEstado con
    // Pageable).
    // Sobrecarga del findByEstado(estado, Pageable) de arriba, sin paginar
    // (Spring Data los distingue por la lista de parámetros).
    List<Estudiante> findByEstado(EstadoLogico estado);

    @Query("SELECT e FROM Estudiante e JOIN e.grupos g WHERE g.idGrupo = :idGrupo AND e.estado = :estado")
    List<Estudiante> findByGrupoAndEstadoList(@Param("idGrupo") Integer idGrupo,
                                              @Param("estado") EstadoLogico estado);

    @Query("""
        SELECT DISTINCT e FROM Estudiante e
        JOIN e.grupos g JOIN g.carrera c
        WHERE c.idCarrera = :idCarrera AND e.estado = :estado
        """)
    List<Estudiante> findByCarreraAndEstadoList(@Param("idCarrera") Integer idCarrera,
                                                @Param("estado") EstadoLogico estado);
}