package com.fides.sienep.repository;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

// Filtros del listado de estudiantes (GET /estudiantes, RF08). Cada uno es opcional y se combinan con AND, así que
// "texto + carrera + estado" devuelve los que cumplen las tres condiciones. Mismas reglas que las queries que
// reemplazan (EstudianteRepository.buscarPorTexto, findByGrupo, findByCarrera, findByItr).
public final class EstudianteSpecs {

    private EstudianteSpecs() {
    }

    // Nombre, apellido o documento que contengan el texto (parcial, sin distinguir mayúsculas).
    public static Specification<Estudiante> texto(String texto) {
        return (root, query, cb) -> {
            String patron = "%" + texto.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("nombre")), patron),
                    cb.like(cb.lower(root.get("apellido")), patron),
                    cb.like(cb.lower(root.get("documento")), patron));
        };
    }

    public static Specification<Estudiante> estado(EstadoLogico estado) {
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    // Grupo, carrera e ITR se resuelven con una subquery (idUsuario IN ...) en vez de un JOIN sobre el root: un
    // estudiante en dos grupos de la misma carrera aparecería dos veces, y con paginación el DISTINCT complica el
    // conteo. Así cada estudiante sale una sola vez y el total de la página es exacto.
    public static Specification<Estudiante> grupo(Integer idGrupo) {
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<Estudiante> e = sub.from(Estudiante.class);
            sub.select(e.get("idUsuario")).where(cb.equal(e.join("grupos").get("idGrupo"), idGrupo));
            return root.get("idUsuario").in(sub);
        };
    }

    public static Specification<Estudiante> carrera(Integer idCarrera) {
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<Estudiante> e = sub.from(Estudiante.class);
            Join<Object, Object> carrera = e.join("grupos").join("carrera");
            sub.select(e.get("idUsuario")).where(cb.equal(carrera.get("idCarrera"), idCarrera));
            return root.get("idUsuario").in(sub);
        };
    }

    public static Specification<Estudiante> itr(Integer idItr) {
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<Estudiante> e = sub.from(Estudiante.class);
            Join<Object, Object> itrCarrera = e.join("grupos").join("carrera").join("itrs");
            sub.select(e.get("idUsuario")).where(cb.equal(itrCarrera.get("id").get("idItr"), idItr));
            return root.get("idUsuario").in(sub);
        };
    }
}
