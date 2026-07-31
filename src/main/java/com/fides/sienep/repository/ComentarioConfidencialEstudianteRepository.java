package com.fides.sienep.repository;

import com.fides.sienep.model.ComentarioConfidencialEstudiante;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComentarioConfidencialEstudianteRepository
        extends JpaRepository<ComentarioConfidencialEstudiante, Integer> {

    Page<ComentarioConfidencialEstudiante> findByEstudianteIdUsuarioAndEstado(
            Integer idEstudiante, EstadoLogico estado, Pageable pageable);
}