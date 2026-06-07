package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.InformeAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InformeAdjuntoRepository extends JpaRepository<InformeAdjunto, Integer> {

    List<InformeAdjunto> findByEstudianteIdUsuarioAndEstado(
            Integer idUsuario, EstadoLogico estado);

    boolean existsByEstudianteIdUsuarioAndNombre(Integer idUsuario, String nombre);
}