package com.fides.sienep.repository;

import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.model.InstanciaComun.InstanciaComunId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstanciaComunRepository extends JpaRepository<InstanciaComun, InstanciaComunId> {

    @Query("SELECT ic FROM InstanciaComun ic WHERE ic.instancia.codInstancia = :codInstancia")
    List<InstanciaComun> findByCodInstancia(@Param("codInstancia") int codInstancia);

    @Query("SELECT ic FROM InstanciaComun ic WHERE ic.categoria.idCategoria = :idCategoria")
    List<InstanciaComun> findByIdCategoria(@Param("idCategoria") int idCategoria);

    @Query("SELECT CASE WHEN COUNT(ic) > 0 THEN true ELSE false END FROM InstanciaComun ic WHERE ic.instancia.codInstancia = :codInstancia AND ic.categoria.idCategoria = :idCategoria")
    boolean existsByCodInstanciaAndIdCategoria(@Param("codInstancia") int codInstancia, @Param("idCategoria") int idCategoria);
}