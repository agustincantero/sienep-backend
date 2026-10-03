package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.InstanciaComun;
import org.springframework.data.jpa.domain.Specification;

// Filtros del listado de instancias (GET /instancias). Cada uno es opcional y se combinan con AND, así que
// "estudiante + categoría + estado" devuelve las que cumplen las tres condiciones. Antes el controller elegía una
// única query según el primer filtro presente y descartaba los demás sin avisar.
public final class InstanciaComunSpecs {

    private InstanciaComunSpecs() {
    }

    public static Specification<InstanciaComun> estado(EstadoLogico estado) {
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<InstanciaComun> estudiante(int idEstudiante) {
        return (root, query, cb) -> cb.equal(root.get("estudiante").get("idUsuario"), idEstudiante);
    }

    public static Specification<InstanciaComun> funcionario(int idFuncionario) {
        return (root, query, cb) -> cb.equal(root.get("funcionario").get("idUsuario"), idFuncionario);
    }

    public static Specification<InstanciaComun> categoria(int idCategoria) {
        return (root, query, cb) -> cb.equal(root.get("categoria").get("idCategoria"), idCategoria);
    }
}
