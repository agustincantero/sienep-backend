package com.fides.sienep.repository;

import com.fides.sienep.model.CategoriaInstancia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaInstanciaRepository extends JpaRepository<CategoriaInstancia, Integer> {
    
    // Método para validar que no existan categorías duplicadas
    boolean existsByNomCategoria(String nomCategoria);
}
