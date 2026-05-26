package com.fides.sienep.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que mapea la tabla proyecto.grupos.
 *
 * Tabla:
 *   id_grupo      SERIAL PK
 *   id_carrera    INT FK -> carreras
 *   nom_grupo     VARCHAR(60) NOT NULL
 *   generacion    INT NOT NULL  (año, ej: 2024)
 *   estado        estado_logico
 *   created_at    TIMESTAMP
 *   updated_at    TIMESTAMP
 *
 * Lombok en entidades JPA — regla clave:
 *   NO usar @Data. Genera equals/hashCode con todos los campos, incluyendo
 *   colecciones lazy, lo que causa LazyInitializationException y stack overflow.
 *   En su lugar: @Getter + @Setter + @Builder + constructores explícitos.
 */
@Entity
@Table(name = "grupos", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "carrera")   // evita ciclo si Carrera también referencia Grupo
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Schema(description = "Grupo académico asociado a una carrera")
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // SERIAL en PostgreSQL
    @Column(name = "id_grupo")
    @EqualsAndHashCode.Include  // solo la PK define identidad del objeto en JPA
    @Schema(description = "Identificador único del grupo", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Integer idGrupo;

    /**
     * FK a la tabla carreras. LAZY porque no siempre se necesita la carrera completa.
     * Siempre preferir LAZY por defecto; usar JOIN FETCH en queries cuando se necesite.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_carrera", nullable = false)
    @Schema(description = "Carrera a la que pertenece el grupo")
    private Carrera carrera;

    @Column(name = "nom_grupo", nullable = false, length = 60)
    @Schema(description = "Nombre del grupo", example = "Grupo A - Turno Mañana")
    private String nomGrupo;

    @Column(name = "generacion", nullable = false)
    @Schema(description = "Año de generación del grupo", example = "2024")
    private Integer generacion;

    @Enumerated(EnumType.STRING)  // almacena el nombre del enum como String, no el ordinal
    @Column(name = "estado", nullable = false)
    @Builder.Default  // necesario con @Builder para que el default se aplique
    @Schema(description = "Estado lógico del grupo", example = "ACTIVO")
    private EstadoLogico estado = EstadoLogico.ACTIVO;

    /**
     * @CreationTimestamp: Hibernate asigna LocalDateTime.now() en el INSERT.
     * updatable = false: después del primer insert, JPA nunca sobreescribe este campo.
     * El trigger SQL también lo setea — ambos llegan al mismo valor.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Fecha de creación")
    private LocalDateTime createdAt;

    /**
     * @UpdateTimestamp: Hibernate actualiza en cada UPDATE.
     * El trigger SQL también lo actualiza — sin conflicto.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Fecha de última actualización")
    private LocalDateTime updatedAt;
}