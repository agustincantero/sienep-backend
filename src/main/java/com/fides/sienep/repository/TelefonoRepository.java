package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Telefono;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TelefonoRepository extends JpaRepository<Telefono, Telefono.Id> {

    List<Telefono> findByIdIdUsuario(Integer idUsuario);

    List<Telefono> findByIdIdUsuarioAndEstado(Integer idUsuario, EstadoLogico estado);
}