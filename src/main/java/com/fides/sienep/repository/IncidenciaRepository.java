package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidenciaRepository extends JpaRepository<Incidencia, Integer> {

    List<Incidencia> findByEstado(EstadoLogico estado);

    Optional<Incidencia> findByCodInstanciaAndEstado(int codInstancia, EstadoLogico estado);

    @Query("SELECT i FROM Incidencia i WHERE i.estudiante.idUsuario = :idEstudiante AND i.estado = :estado")
    List<Incidencia> findByEstudianteAndEstado(@Param("idEstudiante") int idEstudiante, @Param("estado") EstadoLogico estado);

    @Query("SELECT i FROM Incidencia i WHERE i.funcionario.idUsuario = :idFuncionario AND i.estado = :estado")
    List<Incidencia> findByFuncionarioAndEstado(@Param("idFuncionario") int idFuncionario, @Param("estado") EstadoLogico estado);
}