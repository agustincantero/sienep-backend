package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.InstanciaComun;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

// Filtros del listado de instancias (GET /instancias). Cada uno es opcional y se combinan con AND, así que
// "estudiante + categoría + estado" devuelve las que cumplen las tres condiciones. Antes el controller elegía una
// única query según el primer filtro presente y descartaba los demás sin avisar.
public final class InstanciaComunSpecs {

    private InstanciaComunSpecs() {
    }

    // Una colección y no un único estado: "Estado: Todos" del listado pide ACTIVO e INACTIVO en una sola
    // consulta, para que la paginación sea una sola.
    public static Specification<InstanciaComun> estados(Collection<EstadoLogico> estados) {
        return (root, query, cb) -> root.get("estado").in(estados);
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
