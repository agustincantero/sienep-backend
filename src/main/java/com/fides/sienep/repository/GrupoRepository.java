package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrupoRepository extends JpaRepository<Grupo, Integer> {

    List<Grupo> findByEstado(EstadoLogico estado);

    List<Grupo> findByCarreraIdCarreraAndEstado(Integer idCarrera, EstadoLogico estado);

    List<Grupo> findByGeneracionAndEstado(Integer generacion, EstadoLogico estado);

    List<Grupo> findByCarreraIdCarreraAndGeneracionAndEstado(Integer idCarrera, Integer generacion, EstadoLogico estado);

    boolean existsByNomGrupoAndCarreraIdCarreraAndGeneracionAndEstado(
            String nomGrupo, Integer idCarrera, Integer generacion, EstadoLogico estado);
}