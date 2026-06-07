package com.fides.sienep.repository;

import com.fides.sienep.model.ComentarioConfidencialEstudiante;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioConfidencialEstudianteRepository
        extends JpaRepository<ComentarioConfidencialEstudiante, Integer> {

    List<ComentarioConfidencialEstudiante> findByEstudianteIdUsuarioAndEstado(
            Integer idEstudiante, EstadoLogico estado);
}