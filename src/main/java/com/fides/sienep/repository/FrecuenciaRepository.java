package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Frecuencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FrecuenciaRepository extends JpaRepository<Frecuencia, Integer> {

    List<Frecuencia> findByEstado(EstadoLogico estado);

    Optional<Frecuencia> findByIdFrecuenciaAndEstado(int idFrecuencia, EstadoLogico estado);
}