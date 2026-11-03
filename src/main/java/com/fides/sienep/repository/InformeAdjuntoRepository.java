package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.InformeAdjunto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface InformeAdjuntoRepository extends JpaRepository<InformeAdjunto, Integer> {

    Page<InformeAdjunto> findByEstudianteIdUsuarioAndEstado(
            Integer idUsuario, EstadoLogico estado, Pageable pageable);

    Page<InformeAdjunto> findByEstudianteIdUsuarioAndEstadoIn(
            Integer idUsuario, Collection<EstadoLogico> estados, Pageable pageable);

    boolean existsByEstudianteIdUsuarioAndNombreAndEstadoNot(Integer idUsuario, String nombre, EstadoLogico estado);

    Optional<InformeAdjunto> findByIdInformeAndEstado(Integer idInforme, EstadoLogico estado);

    // Autoservicio del estudiante: el informe solo se encuentra si es suyo.
    Optional<InformeAdjunto> findByIdInformeAndEstudianteIdUsuarioAndEstado(
            Integer idInforme, int idUsuario, EstadoLogico estado);
}