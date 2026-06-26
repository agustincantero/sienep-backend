package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    List<Funcionario> findByEstado(
            EstadoLogico estado
    );

    boolean existsByDocumento(
            String documento
    );

    boolean existsByEmail(
            String email
    );
}
