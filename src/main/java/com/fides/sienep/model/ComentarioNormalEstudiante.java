package com.fides.sienep.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Comentario observable (público dentro del sistema) sobre un estudiante.
 * Mapea la tabla proyecto.estu_obs_comentarios.
 *
 * Tabla:
 *   id_comentario  SERIAL  ← PK (autoincremental)
 *   id_usuario     INT FK -> estudiantes
 *   comentario     TEXT NOT NULL
 *   estado         estado_logico
 *   created_at     TIMESTAMP
 *   updated_at     TIMESTAMP
 *   id_autor       INT FK -> funcionarios
 *
 *   PRIMARY KEY (id_comentario, id_usuario)
 *
 * DECISIÓN DE DISEÑO — PK COMPUESTA vs. PK SIMPLE:
 * --------------------------------------------------
 * La DB define PK (id_comentario, id_usuario). Sin embargo, id_comentario
 * es SERIAL (auto-incremental), lo que garantiza su unicidad a nivel de tabla
 * sin necesidad del segundo campo.
 *
 * En JPA usamos SOLO id_comentario como @Id por estas razones:
 *  1. Es técnicamente suficiente para identificar cualquier fila.
 *  2. Evita @IdClass o @EmbeddedId que complicarían el código innecesariamente.
 *  3. La constraint compuesta sigue existiendo a nivel DB para la integridad.
 *
 * Tradeoff: JPA no "conoce" la parte id_usuario de la PK, pero eso no afecta
 * ninguna operación normal (CRUD, queries, relaciones).
 *
 * @AttributeOverrides:
 * --------------------------------------------------
 * Sobreescribimos los nombres de columna heredados de ComentarioBase que
 * difieren en esta tabla específica. Solo sobreescribimos lo que cambia;
 * el resto (estado, created_at, updated_at, id_autor) se hereda tal cual.
 */
@Entity
@Table(name = "estu_obs_comentarios", schema = "proyecto")
@AttributeOverrides({
        // El campo 'contenido' en ComentarioBase usa column="contenido" como placeholder.
        // En esta tabla la columna se llama 'comentario'.
        @AttributeOverride(name = "contenido", column = @Column(name = "comentario", nullable = false, columnDefinition = "TEXT"))
})
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"estudiante", "autor"})  // excluimos relaciones para evitar LazyInitializationException
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Schema(description = "Comentario observable de un funcionario sobre un estudiante")
public class ComentarioNormalEstudiante extends ComentarioBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comentario")
    private Integer id;

    /**
     * Relación con el estudiante comentado.
     * mappedBy en el @OneToMany de Estudiante apunta a este campo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    @Schema(description = "Estudiante sobre el que se realiza el comentario")
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_autor", nullable = false)
    private Funcionario autor;

    // =============================================================
    // CONSTRUCTORES
    // =============================================================

    /**
     * Constructor para crear un nuevo comentario (sin ID todavía).
     */
    public ComentarioNormalEstudiante(String contenido, Funcionario autor, Estudiante estudiante) {
        super(contenido);
        this.autor = autor;
        this.estudiante = estudiante;
    }

    /**
     * Constructor completo para reconstrucción desde DB.
     */
    public ComentarioNormalEstudiante(int idComentario, String comentario, Funcionario autor,
                                      Estudiante estudiante, EstadoLogico estado, LocalDateTime createdAt) {
        super(comentario);
        this.autor = autor;
        this.id = idComentario;
        this.estudiante = estudiante;
        this.estado = estado;
        this.createdAt = createdAt;
    }

    // EqualsAndHashCode basado solo en el ID heredado de ComentarioBase
    // Si el ID es null (entidad nueva, no persistida aún), equals/hashCode
    // usan la identidad del objeto (comportamiento por defecto de Object).
    @EqualsAndHashCode.Include
    public Integer getIdForEquality() {
        return this.id;
    }
}