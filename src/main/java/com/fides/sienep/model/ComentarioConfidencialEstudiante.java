package com.fides.sienep.model;

import com.fides.sienep.security.DatoConfidencialConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Comentario confidencial sobre un estudiante. Solo accesible por Dirección (RD01).
 * Mapea la tabla proyecto.estu_obs_confidenciales.
 *
 * Tabla:
 *   id_obs_confidencial  SERIAL  ← PK
 *   id_estudiante        INT FK -> estudiantes
 *   obs_confidencial     TEXT NOT NULL
 *   estado               estado_logico
 *   created_at           TIMESTAMP
 *   updated_at           TIMESTAMP
 *   id_autor             INT FK -> funcionarios
 *
 * NOTA DE SEGURIDAD (RD01):
 * --------------------------------------------------
 * Esta entidad contiene datos sensibles. El control de acceso se implementa
 * en la capa de servicio y/o a nivel de Spring Security, NO en la entidad.
 * La entidad solo define el modelo de datos — es responsabilidad del
 * SecurityConfig y los permisos RBAC determinar quién puede ver estos datos.
 *
 * @AttributeOverrides:
 * --------------------------------------------------
 * Misma lógica que ComentarioNormalEstudiante pero con los nombres de columna
 * específicos de estu_obs_confidenciales.
 */
@Entity
@Table(name = "estu_obs_confidenciales", schema = "proyecto")
@AttributeOverrides({
        // obs_confidencial es el nombre real de la columna de contenido en esta tabla
        @AttributeOverride(name = "contenido", column = @Column(name = "obs_confidencial", nullable = false, columnDefinition = "TEXT"))
})
// contenido viene de ComentarioBase, compartido con los comentarios normales: el cifrado se
// aplica acá y no en la superclase (ver CifradorDatosConfidenciales).
@Convert(attributeName = "contenido", converter = DatoConfidencialConverter.class)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"estudiante"})
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Schema(description = "Comentario confidencial sobre un estudiante — acceso restringido a Dirección")
public class ComentarioConfidencialEstudiante extends ComentarioBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_obs_confidencial")
    private Integer id;

    /**
     * Relación con el estudiante.
     * Nota: el schema usa 'id_estudiante' como nombre de columna FK
     * (a diferencia de ComentarioNormalEstudiante que usa 'id_usuario').
     * Esto es una inconsistencia del schema — aquí lo mapeamos fielmente.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    @Schema(description = "Estudiante sobre el que se registra la observación confidencial")
    private Estudiante estudiante;

    // =============================================================
    // CONSTRUCTORES
    // =============================================================

    /**
     * Constructor para crear nueva observación confidencial.
     */
    public ComentarioConfidencialEstudiante(String contenido, Funcionario autor, Estudiante estudiante) {
        super(contenido);
        this.autor = autor;
        this.estudiante = estudiante;
    }

    /**
     * Constructor para reconstrucción desde DB.
     */
    public ComentarioConfidencialEstudiante(Integer id, String contenido, Funcionario autor,
                                            Estudiante estudiante) {
        super(contenido);
        this.autor = autor;
        this.id = id;
        this.estudiante = estudiante;
    }

    /**
     * Constructor con estado explícito (mapeando el boolean del original).
     * Se convierte boolean → EstadoLogico aquí para mantener compatibilidad
     * con código existente, aunque idealmente el original también debería
     * usar EstadoLogico directamente.
     */
    public ComentarioConfidencialEstudiante(int id, String contenido, Funcionario autor,
                                            Estudiante estudiante, boolean activo, LocalDateTime fechaHora) {
        super(contenido);
        this.autor = autor;
        this.id = id;
        this.estudiante = estudiante;
        this.estado = activo ? EstadoLogico.ACTIVO : EstadoLogico.INACTIVO;
        this.createdAt = fechaHora;
    }

    @EqualsAndHashCode.Include
    public Integer getIdForEquality() {
        return this.id;
    }
}