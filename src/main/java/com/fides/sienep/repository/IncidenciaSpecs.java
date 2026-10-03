package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Incidencia;
import org.springframework.data.jpa.domain.Specification;

// Filtros del listado de incidencias (GET /incidencias). Cada uno es opcional y se combinan con AND, así que
// "estudiante + funcionario + estado" devuelve las que cumplen las tres condiciones. Antes el controller elegía una
// única query según el primer filtro presente y descartaba los demás sin avisar.
public final class IncidenciaSpecs {

    private IncidenciaSpecs() {
    }

    public static Specification<Incidencia> estado(EstadoLogico estado) {
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Incidencia> estudiante(int idEstudiante) {
        return (root, query, cb) -> cb.equal(root.get("estudiante").get("idUsuario"), idEstudiante);
    }

    public static Specification<Incidencia> funcionario(int idFuncionario) {
        return (root, query, cb) -> cb.equal(root.get("funcionario").get("idUsuario"), idFuncionario);
    }
}
