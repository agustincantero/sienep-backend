package com.fides.sienep.repository;

import com.fides.sienep.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer>, JpaSpecificationExecutor<Auditoria> {
}
