package com.fides.sienep.repository;

import com.fides.sienep.model.ComentarioConfidencialInstancia;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComentarioConfidencialInstanciaRepository
        extends JpaRepository<ComentarioConfidencialInstancia, ComentarioConfidencialInstancia.ComentarioConfidencialInstanciaId> {

    List<ComentarioConfidencialInstancia> findByInstancia_CodInstanciaAndEstado(
            int codInstancia, EstadoLogico estado);

    Optional<ComentarioConfidencialInstancia> findById_IdComConfidencialAndEstado(
            int idComConfidencial, EstadoLogico estado);
}