package com.fides.sienep.repository;

import com.fides.sienep.model.FuncionarioGcal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FuncionarioGcalRepository extends JpaRepository<FuncionarioGcal, Integer> {

    Optional<FuncionarioGcal> findByFuncionarioIdUsuario(int idUsuario);
}