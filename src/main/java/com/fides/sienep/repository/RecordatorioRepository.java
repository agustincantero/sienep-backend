package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Recordatorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecordatorioRepository extends JpaRepository<Recordatorio, Integer> {

    List<Recordatorio> findByEstado(EstadoLogico estado);

    Optional<Recordatorio> findByCodRecordatorioAndEstado(int codRecordatorio, EstadoLogico estado);

    @Query("SELECT r FROM Recordatorio r WHERE r.instancia.codInstancia = :codInstancia AND r.estado = :estado")
    List<Recordatorio> findByInstanciaAndEstado(@Param("codInstancia") int codInstancia, @Param("estado") EstadoLogico estado);

    @Query("SELECT r FROM Recordatorio r WHERE r.responsable.idUsuario = :idResponsable AND r.estado = :estado")
    List<Recordatorio> findByResponsableAndEstado(@Param("idResponsable") int idResponsable, @Param("estado") EstadoLogico estado);
}