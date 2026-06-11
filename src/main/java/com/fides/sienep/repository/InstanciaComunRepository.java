package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.InstanciaComun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstanciaComunRepository extends JpaRepository<InstanciaComun, Integer> {

    List<InstanciaComun> findByEstado(EstadoLogico estado);

    Optional<InstanciaComun> findByCodInstanciaAndEstado(int codInstancia, EstadoLogico estado);

    @Query("SELECT ic FROM InstanciaComun ic WHERE ic.estudiante.idUsuario = :idEstudiante AND ic.estado = :estado")
    List<InstanciaComun> findByEstudianteAndEstado(@Param("idEstudiante") int idEstudiante,
                                                   @Param("estado") EstadoLogico estado);

    @Query("SELECT ic FROM InstanciaComun ic WHERE ic.funcionario.idUsuario = :idFuncionario AND ic.estado = :estado")
    List<InstanciaComun> findByFuncionarioAndEstado(@Param("idFuncionario") int idFuncionario,
                                                    @Param("estado") EstadoLogico estado);

    @Query("SELECT ic FROM InstanciaComun ic WHERE ic.categoria.idCategoria = :idCategoria AND ic.estado = :estado")
    List<InstanciaComun> findByCategoriaAndEstado(@Param("idCategoria") int idCategoria,
                                                  @Param("estado") EstadoLogico estado);
}