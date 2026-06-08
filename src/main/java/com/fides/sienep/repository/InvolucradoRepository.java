package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Involucrado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvolucradoRepository
        extends JpaRepository<Involucrado, Involucrado.InvolucradoId> {

    List<Involucrado> findByIdCodInstanciaAndEstado(
            int codInstancia, EstadoLogico estado);

    Optional<Involucrado> findByIdCodInstanciaAndIdNomInvolucrado(
            int codInstancia, String nomInvolucrado);
}