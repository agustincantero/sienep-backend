package com.fides.sienep.repository;

import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository de Estudiante.
 *
 * Extiende JpaRepository<Estudiante, Integer>:
 *   - Estudiante : la entidad que gestiona.
 *   - Integer    : tipo del @Id (idUsuario heredado de Usuario).
 *
 * JpaRepository provee sin escribir nada:
 *   save(), findById(), findAll(), deleteById(), existsById(),
 *   findAll(Pageable), count(), y más.
 *
 * HERENCIA JOINED y queries:
 *   Los campos nombre, apellido, documento, email, estado viven
 *   en la tabla 'usuarios', pero Estudiante los hereda de Usuario.
 *   Hibernate genera el JOIN entre ambas tablas automáticamente
 *   en cada consulta — no hay que escribirlo manualmente.
 *
 * VISIBILIDAD POR ROL:
 *   El Repository no decide qué datos ve cada rol.
 *   Esa lógica vive en el Service, que llama al método correcto
 *   según el contexto de seguridad del usuario autenticado.
 */
@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Integer> {

    // =========================================================
    // BÚSQUEDA POR DOCUMENTO (CI)
    // =========================================================

    /**
     * Búsqueda exacta por número de documento (CI).
     * Devuelve Optional: el documento es único, hay cero o un resultado.
     *
     * Método derivado: Spring Data genera el SQL por el nombre del método.
     * No necesita @Query.
     */
    Optional<Estudiante> findByDocumento(String documento);

    // =========================================================
    // BÚSQUEDA POR NOMBRE / APELLIDO (parcial)
    // =========================================================

    /**
     * Búsqueda de texto libre sobre nombre y apellido.
     *
     * Por qué @Query y no método derivado:
     *   El método derivado requeriría pasar el mismo texto dos veces:
     *   findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(texto, texto, pageable)
     *   Con @Query usamos :texto una sola vez — más limpio.
     *
     * LOWER + CONCAT('%',:texto,'%'): parcial e insensible a mayúsculas.
     * Ejemplo: "mar" encuentra "María", "Marcos", "Amparo".
     */
    @Query("""
            SELECT e FROM Estudiante e
            WHERE LOWER(e.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(e.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    Page<Estudiante> buscarPorNombre(@Param("texto") String texto, Pageable pageable);

    /**
     * Búsqueda por nombre/apellido filtrada por estado.
     * Para roles sin privilegios que solo ven estudiantes activos.
     */
    @Query("""
            SELECT e FROM Estudiante e
            WHERE (LOWER(e.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(e.apellido) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND e.estado = :estado
            """)
    Page<Estudiante> buscarPorNombreYEstado(@Param("texto") String texto,
                                            @Param("estado") EstadoLogico estado,
                                            Pageable pageable);

    // =========================================================
    // LISTADO CON FILTRO DE ESTADO
    // =========================================================

    /**
     * Lista estudiantes filtrados por estado con paginación.
     *
     * Roles sin privilegios  → llaman con estado = ACTIVO.
     * Roles con acceso total → usan findAll(Pageable) de JpaRepository.
     */
    Page<Estudiante> findByEstado(EstadoLogico estado, Pageable pageable);

    // =========================================================
    // FILTROS POR GRUPO, CARRERA E ITR
    // =========================================================

    /**
     * Lista estudiantes de un grupo específico con paginación.
     *
     * JOIN e.grupos g: JPQL navega la relación @ManyToMany de Estudiante.
     * Hibernate genera el JOIN a través de estu_grupos automáticamente.
     */
    @Query("""
            SELECT e FROM Estudiante e
            JOIN e.grupos g
            WHERE g.idGrupo = :idGrupo
              AND e.estado = :estado
            """)
    Page<Estudiante> findByGrupoAndEstado(@Param("idGrupo") Integer idGrupo,
                                          @Param("estado") EstadoLogico estado,
                                          Pageable pageable);

    /**
     * Lista estudiantes de una carrera específica con paginación.
     * Navega: Estudiante → Grupo → Carrera.
     *
     * DISTINCT: un estudiante puede estar en varios grupos de la
     * misma carrera — sin DISTINCT aparecería duplicado.
     */
    @Query("""
            SELECT DISTINCT e FROM Estudiante e
            JOIN e.grupos g
            JOIN g.carrera c
            WHERE c.idCarrera = :idCarrera
              AND e.estado = :estado
            """)
    Page<Estudiante> findByCarreraAndEstado(@Param("idCarrera") Integer idCarrera,
                                            @Param("estado") EstadoLogico estado,
                                            Pageable pageable);

    /**
     * Lista estudiantes de un ITR específico con paginación.
     *
     * Camino confirmado por el schema:
     *   Estudiante → (estu_grupos) → Grupo → Carrera → (itr_carrera) → ITR
     *
     * itr_carrera es una tabla join entre itrs y carreras:
     * una carrera puede dictarse en varios ITRs.
     *
     * Cada JOIN navega un tramo de esa cadena:
     *   JOIN e.grupos g     → atraviesa estu_grupos
     *   JOIN g.carrera c    → llega a carreras
     *   JOIN c.itrs itr     → atraviesa itr_carrera y llega a itrs
     *
     * DISTINCT: necesario porque el estudiante puede tener
     * varios grupos que llevan a la misma carrera/ITR.
     *
     * PREREQUISITO: Carrera debe tener @ManyToMany con ITR
     * mapeado sobre la tabla itr_carrera.
     */
    @Query("""
            SELECT DISTINCT e FROM Estudiante e
            JOIN e.grupos g
            JOIN g.carrera c
            JOIN c.itrs itr
            WHERE itr.idItr = :idItr
              AND e.estado = :estado
            """)
    Page<Estudiante> findByItrAndEstado(@Param("idItr") Integer idItr,
                                        @Param("estado") EstadoLogico estado,
                                        Pageable pageable);

    // =========================================================
    // VALIDACIONES DE UNICIDAD
    // =========================================================

    /**
     * Verifica si ya existe un estudiante con ese documento.
     * El Service lo usa antes de crear un estudiante nuevo para
     * lanzar un error de negocio claro en vez de dejar que la
     * constraint de la BD lance una excepción genérica.
     */
    boolean existsByDocumento(String documento);

    /**
     * Verifica si ya existe un usuario con ese email.
     * El campo email está en Usuario pero Spring Data JPA lo
     * resuelve por la herencia — no necesita query manual.
     */
    boolean existsByEmail(String email);
}