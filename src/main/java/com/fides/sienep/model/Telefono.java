package com.fides.sienep.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que mapea la tabla proyecto.estu_telefonos.
 *
 * Tabla:
 *   id_usuario  INT FK -> estudiantes  <- parte de PK
 *   telefono    VARCHAR(15)            <- parte de PK
 *   estado      estado_logico
 *   created_at  TIMESTAMP
 *   updated_at  TIMESTAMP
 *
 *   PRIMARY KEY (id_usuario, telefono)
 *
 * ESTRATEGIA DE PK COMPUESTA: @EmbeddedId
 * ----------------------------------------
 * La PK completa vive en TelefonoId (clase @Embeddable).
 * Esta entidad tiene un unico campo @EmbeddedId que la contiene.
 *
 * Acceso a los campos de la PK:
 *   telefono.getId().getIdUsuario()  -> id del estudiante
 *   telefono.getId().getNumero()     -> numero de telefono
 *
 * @MapsId("idUsuario"):
 *   Vincula el campo 'idUsuario' dentro de TelefonoId con la FK
 *   de la relacion @ManyToOne. Hibernate entiende que es la misma
 *   columna (id_usuario) y no necesita insertable/updatable=false.
 *   Cuando asignas el Estudiante, el idUsuario del embedded ID
 *   se sincroniza automaticamente.
 *
 * REGLA OPERACIONAL DE ESTE SISTEMA:
 *   Los telefonos siempre se operan en el contexto del estudiante.
 *   No existe un endpoint que opere sobre un telefono sin su estudiante.
 *   Por eso la PK compuesta refleja exactamente como el sistema trabaja:
 *   identificas un telefono por quien lo tiene + el numero.
 */
@Entity
@Table(name = "estu_telefonos", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "estudiante")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Schema(description = "Telefono asociado a un estudiante")
public class Telefono {

    @EmbeddedId
    @EqualsAndHashCode.Include
    @Schema(description = "Clave primaria compuesta: id del estudiante + numero de telefono")
    private TelefonoId id;

    /**
     * @MapsId("idUsuario"): le dice a JPA que el campo 'idUsuario' dentro
     * de TelefonoId corresponde a la FK de esta relacion.
     * Hibernate gestiona esa columna a traves del @EmbeddedId — no necesita
     * insertable=false, updatable=false como con @IdClass.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idUsuario")
    @JoinColumn(name = "id_usuario", nullable = false)
    @Schema(description = "Estudiante propietario del telefono")
    private Estudiante estudiante;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Schema(description = "Estado logico del telefono", example = "ACTIVO")
    private EstadoLogico estado = EstadoLogico.ACTIVO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    /**
     * Constructor de creacion.
     * Arma el TelefonoId a partir del estudiante y el numero,
     * manteniendo coherencia entre el embedded ID y la relacion.
     */
    public Telefono(Estudiante estudiante, String numero) {
        this.id = new TelefonoId(estudiante.getIdUsuario(), numero);
        this.estudiante = estudiante;
        this.estado = EstadoLogico.ACTIVO;
    }
}