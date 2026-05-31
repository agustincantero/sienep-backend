package com.fides.sienep.repository;

import com.fides.sienep.model.CategoriaRecordatorio;
import com.fides.sienep.model.EstadoLogico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRecordatorioRepository extends JpaRepository<CategoriaRecordatorio, Integer> {
    
    // Método para validar que no existan categorías duplicadas
    boolean existsByNomCategoria(String nomCategoria);

    // Consulta dinámica para filtrar por nombre (parcial) y/o estado
    @Query("SELECT c FROM CategoriaRecordatorio c WHERE " +
           "(CAST(:nombre AS text) IS NULL OR UPPER(c.nomCategoria) LIKE UPPER(CONCAT('%', CAST(:nombre AS text), '%'))) AND " +
           "(CAST(:estado AS text) IS NULL OR c.estado = :estado)")
    List<CategoriaRecordatorio> buscarConFiltros(@Param("nombre") String nombre, @Param("estado") EstadoLogico estado);
}
