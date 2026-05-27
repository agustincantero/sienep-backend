package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Instancia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstanciaRepository extends JpaRepository<Instancia, Integer> {

    List<Instancia> findByEstado(EstadoLogico estado);

    Optional<Instancia> findByCodInstanciaAndEstado(int codInstancia, EstadoLogico estado);
}