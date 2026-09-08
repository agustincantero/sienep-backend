package com.fides.sienep.repository;

import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {

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

    // Búsqueda por nombre, apellido o documento (parcial, sin importar mayúsculas)
    // — versión con y sin filtro de estado.
    @Query("""
            SELECT e FROM Estudiante e
            WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(e.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(e.documento) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<Estudiante> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            SELECT e FROM Estudiante e
            WHERE (LOWER(e.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(e.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(e.documento) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND e.estado = :estado
            """)
    Page<Estudiante> buscarPorTextoYEstado(@Param("texto") String texto,
                                           @Param("estado") EstadoLogico estado,
                                           Pageable pageable);

    // Listado por estado
    Page<Estudiante> findByEstado(EstadoLogico estado, Pageable pageable);

    // Filtro por grupo — con y sin filtro de estado
    @Query("SELECT e FROM Estudiante e JOIN e.grupos g WHERE g.idGrupo = :idGrupo")
    Page<Estudiante> findByGrupo(@Param("idGrupo") Integer idGrupo, Pageable pageable);

    @Query("SELECT e FROM Estudiante e JOIN e.grupos g WHERE g.idGrupo = :idGrupo AND e.estado = :estado")
    Page<Estudiante> findByGrupoAndEstado(@Param("idGrupo") Integer idGrupo,
                                          @Param("estado") EstadoLogico estado,
                                          Pageable pageable);

    // Filtro por carrera — con y sin filtro de estado
    @Query("SELECT DISTINCT e FROM Estudiante e JOIN e.grupos g JOIN g.carrera c WHERE c.idCarrera = :idCarrera")
    Page<Estudiante> findByCarrera(@Param("idCarrera") Integer idCarrera, Pageable pageable);

    @Query("""
            SELECT DISTINCT e FROM Estudiante e
            JOIN e.grupos g JOIN g.carrera c
            WHERE c.idCarrera = :idCarrera AND e.estado = :estado
            """)
    Page<Estudiante> findByCarreraAndEstado(@Param("idCarrera") Integer idCarrera,
                                            @Param("estado") EstadoLogico estado,
                                            Pageable pageable);

    // Filtro por ITR — con y sin filtro de estado
    @Query("""
            SELECT DISTINCT e FROM Estudiante e
            JOIN e.grupos g JOIN g.carrera c JOIN c.itrs itr
            WHERE itr.id.idItr = :idItr
            """)
    Page<Estudiante> findByItr(@Param("idItr") Integer idItr, Pageable pageable);

    @Query("""
            SELECT DISTINCT e FROM Estudiante e
            JOIN e.grupos g JOIN g.carrera c JOIN c.itrs itr
            WHERE itr.id.idItr = :idItr AND e.estado = :estado
            """)
    Page<Estudiante> findByItrAndEstado(@Param("idItr") Integer idItr,
                                        @Param("estado") EstadoLogico estado,
                                        Pageable pageable);

    // Validaciones de unicidad
    boolean existsByDocumento(String documento);
    boolean existsByEmail(String email);


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