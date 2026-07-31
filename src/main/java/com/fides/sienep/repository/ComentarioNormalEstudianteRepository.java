package com.fides.sienep.repository;

import com.fides.sienep.model.ComentarioNormalEstudiante;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComentarioNormalEstudianteRepository
        extends JpaRepository<ComentarioNormalEstudiante, Integer> {

    Page<ComentarioNormalEstudiante> findByEstudianteIdUsuarioAndEstado(
            Integer idUsuario, EstadoLogico estado, Pageable pageable);
}
