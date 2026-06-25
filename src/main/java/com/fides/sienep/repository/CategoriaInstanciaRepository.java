package com.fides.sienep.repository;

import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaInstanciaRepository extends JpaRepository<CategoriaInstancia, Integer> {

    boolean existsByNomCategoria(String nomCategoria);

    @Query("SELECT c FROM CategoriaInstancia c WHERE " +
            "(CAST(:nombre AS text) IS NULL OR UPPER(c.nomCategoria) LIKE UPPER(CONCAT('%', CAST(:nombre AS text), '%'))) AND " +
            "(CAST(:estado AS text) IS NULL OR c.estado = :estado)")
    List<CategoriaInstancia> buscarConFiltros(@Param("nombre") String nombre, @Param("estado") EstadoLogico estado);

    @Query("SELECT COUNT(i) > 0 FROM InstanciaComun i " +
            "WHERE i.categoria.idCategoria = :idCategoria AND i.estado = 'ACTIVO'")
    boolean existeInstanciaActivaConCategoria(@Param("idCategoria") int idCategoria);
}