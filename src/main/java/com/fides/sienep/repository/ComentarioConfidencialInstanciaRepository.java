package com.fides.sienep.repository;

import com.fides.sienep.model.ComentarioConfidencialInstancia;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComentarioConfidencialInstanciaRepository
        extends JpaRepository<ComentarioConfidencialInstancia, ComentarioConfidencialInstancia.ComentarioConfidencialInstanciaId> {

    Page<ComentarioConfidencialInstancia> findByInstancia_CodInstanciaAndEstado(
            int codInstancia, EstadoLogico estado, Pageable pageable);

    Optional<ComentarioConfidencialInstancia> findById_IdComConfidencialAndEstado(
            int idComConfidencial, EstadoLogico estado);
}