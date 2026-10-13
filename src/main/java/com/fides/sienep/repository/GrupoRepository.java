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

    // IgnoreCase: "Grupo A" y "grupo a" son el mismo grupo (mismo criterio que carreras e ITRs).
    boolean existsByNomGrupoIgnoreCaseAndCarreraIdCarreraAndGeneracionAndEstado(
            String nomGrupo, Integer idCarrera, Integer generacion, EstadoLogico estado);

    // Para editar: el mismo chequeo, sin contar al propio grupo.
    boolean existsByNomGrupoIgnoreCaseAndCarreraIdCarreraAndGeneracionAndEstadoAndIdGrupoNot(
            String nomGrupo, Integer idCarrera, Integer generacion, EstadoLogico estado, Integer idGrupo);

    // Comparación exacta, igual que uk_grupo_carrera_grupo_generacion: sirve para anticipar el rechazo de esa
    // restricción (que no distingue activos de inactivos) y responder con un mensaje claro.
    boolean existsByNomGrupoAndCarreraIdCarreraAndGeneracionAndEstado(
            String nomGrupo, Integer idCarrera, Integer generacion, EstadoLogico estado);
}