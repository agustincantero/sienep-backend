package com.fides.sienep.repository;

import com.fides.sienep.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
