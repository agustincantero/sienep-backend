package com.fides.sienep.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entidad que mapea la tabla proyecto.estu_informesadjs.
 *
 * Tabla:
 *   id_informe   SERIAL PK
 *   id_usuario   INT FK -> estudiantes (NO a usuarios directamente)
 *   url_archivo  VARCHAR(255) NOT NULL
 *   ruta         VARCHAR(255) NULLABLE
 *   nom_archivo  VARCHAR(70)  NOT NULL
 *   estado       estado_logico NOT NULL DEFAULT 'ACTIVO'
 *   created_at   TIMESTAMP
 *   updated_at   TIMESTAMP
 *
 * CAMBIO RESPECTO AL ORIGINAL:
 *   Se eliminan los modificadores 'final' en todos los campos.
 *   JPA necesita poder setear los campos via reflexión en la entidad gestionada.
 *   Los campos 'final' rompen el ciclo de vida de Hibernate (proxies, lazy loading).
 */
@Entity
@Table(
        name = "estu_informesadjs",
        schema = "proyecto",
        // Refleja la constraint de unicidad del schema: un estudiante no puede tener
        // dos archivos con el mismo nombre.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_informe_nombre",
                columnNames = {"id_usuario", "nom_archivo"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "estudiante")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Schema(description = "Informe adjunto asociado a un estudiante")
public class InformeAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_informe")
    @EqualsAndHashCode.Include
    @Schema(description = "Identificador único del informe", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Integer idInforme;

    /**
     * FK a proyecto.estudiantes (NO a usuarios).
     * El schema define: fk_informe_estudiante -> proyecto.estudiantes(id_usuario)
     * Esto es intencional: los informes son exclusivos de estudiantes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    @Schema(description = "Estudiante al que pertenece el informe")
    private Estudiante estudiante;

    /**
     * Nombre físico del archivo guardado en disco (UUID + extensión original),
     * generado internamente por InformeAdjuntoService al subir el MultipartFile
     * (nunca lo define el cliente). Reutiliza la columna histórica url_archivo
     * -antes pensada para una URL externa- para no requerir una migración del
     * schema; el schema valida igualmente su extensión (PDF, JPG, JPEG, PNG).
     */
    @Column(name = "url_archivo", nullable = false, length = 255)
    @Schema(description = "Nombre físico del archivo en el almacenamiento local", example = "3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.pdf")
    private String urlArchivo;

    /**
     * Subdirectorio (relativo a app.storage.informes-dir) donde vive el
     * archivo físico. Por defecto, el id del estudiante.
     */
    @Column(name = "ruta", length = 255)
    @Schema(description = "Subdirectorio de almacenamiento del archivo", example = "5")
    private String ruta;

    @Column(name = "nom_archivo", nullable = false, length = 70)
    @Schema(description = "Nombre del archivo", example = "informe_psicopedagogico_2024")
    private String nombre;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    @Schema(description = "Estado lógico del informe", example = "ACTIVO")
    private EstadoLogico estado = EstadoLogico.ACTIVO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}