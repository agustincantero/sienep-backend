package com.fides.sienep.repository;

import com.fides.sienep.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    // Validaciones de unicidad
    boolean existsByDocumento(String documento);
    boolean existsByEmail(String email);

    // Para editar(): excluye al propio estudiante, si no cualquier edición que no
    // toque el email fallaría al "chocar" contra su propio registro.
    boolean existsByEmailAndIdUsuarioNot(String email, int idUsuario);
}