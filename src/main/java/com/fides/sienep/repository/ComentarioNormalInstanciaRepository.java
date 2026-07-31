package com.fides.sienep.repository;

import com.fides.sienep.model.ComentarioNormalInstancia;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComentarioNormalInstanciaRepository
        extends JpaRepository<ComentarioNormalInstancia, Integer> {

    Page<ComentarioNormalInstancia> findByInstancia_CodInstanciaAndEstado(
            int codInstancia, EstadoLogico estado, Pageable pageable);

    Optional<ComentarioNormalInstancia> findByIdComentarioAndEstado(
            int idComentario, EstadoLogico estado);
}