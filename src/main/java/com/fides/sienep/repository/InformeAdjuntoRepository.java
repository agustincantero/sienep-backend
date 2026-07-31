package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.InformeAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InformeAdjuntoRepository extends JpaRepository<InformeAdjunto, Integer> {

    List<InformeAdjunto> findByEstudianteIdUsuarioAndEstado(
            Integer idUsuario, EstadoLogico estado);

    boolean existsByEstudianteIdUsuarioAndNombre(Integer idUsuario, String nombre);

    Optional<InformeAdjunto> findByIdInformeAndEstado(Integer idInforme, EstadoLogico estado);
}