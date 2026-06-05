package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Itr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItrRepository extends JpaRepository<Itr, Integer> {

    List<Itr> findByEstado(EstadoLogico estado);

    boolean existsByNomItr(String nomItr);
}