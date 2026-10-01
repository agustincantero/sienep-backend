package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoSolicitud;
import com.fides.sienep.model.SolicitudInstancia;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SolicitudInstanciaRepository extends JpaRepository<SolicitudInstancia, Integer> {

    Page<SolicitudInstancia> findByEstudianteIdUsuario(int idEstudiante, Pageable pageable);

    Page<SolicitudInstancia> findByEstado(EstadoSolicitud estado, Pageable pageable);

    long countByEstudianteIdUsuarioAndEstado(int idEstudiante, EstadoSolicitud estado);

    // Lock pesimista, mismo criterio que RecordatorioRepository.findByCodRecordatorioAndEstadoParaActualizar:
    // evita que dos funcionarios atiendan la misma solicitud a la vez y se generen dos instancias.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SolicitudInstancia s WHERE s.idSolicitud = :idSolicitud")
    Optional<SolicitudInstancia> findByIdParaActualizar(@Param("idSolicitud") int idSolicitud);
}
