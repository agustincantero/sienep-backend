package com.fides.sienep.repository;

import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarreraRepository extends JpaRepository<Carrera, Integer> {

    List<Carrera> findByEstado(EstadoLogico estado);

    boolean existsByNomCarreraIgnoreCase(String nomCarrera);
}