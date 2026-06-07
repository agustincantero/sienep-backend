package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.ItrCarrera;
import com.fides.sienep.model.ItrCarrera.ItrCarreraId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItrCarreraRepository
        extends JpaRepository<ItrCarrera, ItrCarreraId> {

    Optional<ItrCarrera> findByItrIdItrAndCarreraIdCarrera(
            Integer idItr,
            Integer idCarrera
    );

    boolean existsByItrIdItrAndCarreraIdCarrera(
            Integer idItr,
            Integer idCarrera
    );

    List<ItrCarrera> findByEstado(
            EstadoLogico estado
    );

    List<ItrCarrera> findByItrIdItr(
            Integer idItr
    );

    List<ItrCarrera> findByCarreraIdCarrera(
            Integer idCarrera
    );

    List<ItrCarrera> findByItrIdItrAndEstado(
            Integer idItr,
            EstadoLogico estado
    );

    List<ItrCarrera> findByCarreraIdCarreraAndEstado(
            Integer idCarrera,
            EstadoLogico estado
    );
}