package com.fides.sienep.repository;

import com.fides.sienep.model.ComentarioNormalInstancia;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComentarioNormalInstanciaRepository
        extends JpaRepository<ComentarioNormalInstancia, Integer> {

    List<ComentarioNormalInstancia> findByInstancia_CodInstanciaAndEstado(
            int codInstancia, EstadoLogico estado);

    Optional<ComentarioNormalInstancia> findByIdComentarioAndEstado(
            int idComentario, EstadoLogico estado);
}