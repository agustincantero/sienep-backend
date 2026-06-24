package com.fides.sienep.repository;

import com.fides.sienep.model.LogIntento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LogIntentoRepository extends JpaRepository<LogIntento, Integer> {

    @Query("""
        SELECT COUNT(l) FROM LogIntento l
        WHERE l.idUsuario = :idUsuario
          AND l.fueExitoso = false
          AND l.fechaHora >= :desde
        """)
    long contarIntentosFallidosDesde(
            @Param("idUsuario") Integer idUsuario,
            @Param("desde") LocalDateTime desde
    );
}