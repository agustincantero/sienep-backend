package com.fides.sienep.repository;

import com.fides.sienep.model.ReporteGenerado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReporteGeneradoRepository extends JpaRepository<ReporteGenerado, Integer> {

    Optional<ReporteGenerado> findByHashSha256(String hashSha256);
}
