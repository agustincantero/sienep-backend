package com.fides.sienep.repository;

import com.fides.sienep.model.TokenRestablecimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRestablecimientoRepository extends JpaRepository<TokenRestablecimiento, Integer> {

    Optional<TokenRestablecimiento> findByTokenHashAndUsadoFalse(String tokenHash);

    @Modifying
    @Query("UPDATE TokenRestablecimiento t SET t.usado = true WHERE t.idUsuario = :idUsuario AND t.usado = false")
    void invalidarTokensActivos(@Param("idUsuario") int idUsuario);

    @Query("""
        SELECT COUNT(t) FROM TokenRestablecimiento t
        WHERE t.idUsuario = :idUsuario
          AND t.createdAt >= :desde
        """)
    long contarTokensGeneradosDesde(
            @Param("idUsuario") Integer idUsuario,
            @Param("desde") java.time.LocalDateTime desde
    );
}
