package com.fides.sienep.repository;

import com.fides.sienep.model.ComentarioConfidencialInstancia;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface ComentarioConfidencialInstanciaRepository
        extends JpaRepository<ComentarioConfidencialInstancia, Integer> {

    Page<ComentarioConfidencialInstancia> findByInstancia_CodInstanciaAndEstadoIn(
            int codInstancia, Collection<EstadoLogico> estados, Pageable pageable);

    Optional<ComentarioConfidencialInstancia> findByIdComConfidencialAndEstado(
            int idComConfidencial, EstadoLogico estado);
}