package com.fides.sienep.repository;

import com.fides.sienep.model.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Integer> {
    long countByIdPermisoIn(Collection<Integer> idPermisos);
}
