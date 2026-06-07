package com.fides.sienep.repository;

import com.fides.sienep.model.ComentarioNormalEstudiante;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioNormalEstudianteRepository
        extends JpaRepository<ComentarioNormalEstudiante, Integer> {

    List<ComentarioNormalEstudiante> findByEstudianteIdUsuarioAndEstado(
            Integer idUsuario, EstadoLogico estado);
}
