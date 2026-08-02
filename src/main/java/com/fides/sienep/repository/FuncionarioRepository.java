package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Integer> {

    @Query("SELECT f FROM Funcionario f " +
            "JOIN FETCH f.rol r " +
            "JOIN FETCH r.rolPermisos rp " +
            "JOIN FETCH rp.permiso " +
            "WHERE f.email = :email")

    Optional<Funcionario> findByEmail(String email);


    Optional<Funcionario> findByDocumento(String documento);

    Optional<Funcionario> findByDocumentoAndEstado(
            String documento,
            EstadoLogico estado
    );

    Page<Funcionario> findByEstado(
            EstadoLogico estado,
            Pageable pageable
    );

    // Búsqueda por nombre, apellido o documento (parcial, sin importar mayúsculas).
    // A diferencia de Estudiante, acá no hace falta una versión sin filtro de
    // estado: listar() siempre tiene un estado efectivo (por defecto ACTIVO), no
    // existe un permiso equivalente a VER_ESTUDIANTE que habilite "sin filtrar".
    @Query("""
            SELECT f FROM Funcionario f
            WHERE (LOWER(f.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(f.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(f.documento) LIKE LOWER(CONCAT('%', :texto, '%')))
              AND f.estado = :estado
            """)
    Page<Funcionario> buscarPorTextoYEstado(@Param("texto") String texto,
                                            @Param("estado") EstadoLogico estado,
                                            Pageable pageable);

    boolean existsByDocumento(
            String documento
    );

    boolean existsByEmail(
            String email
    );
}
