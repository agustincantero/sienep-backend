package com.fides.sienep.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidad Estudiante. Mapea la tabla proyecto.estudiantes.
 * Herencia JOINED desde proyecto.usuarios.
 *
 * PREREQUISITO en Usuario.java:
 *   @Entity
 *   @Table(name = "usuarios", schema = "proyecto")
 *   @Inheritance(strategy = InheritanceType.JOINED)
 *
 *   @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
 *   @Column(name = "id_usuario")
 *   private Integer idUsuario;
 *
 * FILOSOFIA DE DATOS:
 *   Baja logica siempre. cascade = {PERSIST, MERGE} unicamente.
 *   Nunca CascadeType.REMOVE ni orphanRemoval.
 *   Coherente con ON DELETE RESTRICT del schema.
 *
 * GRUPOS — @ManyToMany:
 *   La tabla estu_grupos tiene campos extra (estado, created_at, updated_at)
 *   gestionados por triggers de BD. Para el scope actual @ManyToMany es
 *   suficiente. Si en el futuro se necesita gestionar 'estado' desde Java,
 *   se introduce la entidad EstuGrupo.
 */
@Entity
@Table(name = "estudiantes", schema = "proyecto")
@PrimaryKeyJoinColumn(name = "id_usuario")
@Getter
@Setter
@NoArgsConstructor
@ToString(
        callSuper = false,
        exclude = {"telefonos", "grupos", "comentarios", "comentariosConfidenciales", "informesAdjuntos"}
)
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Schema(description = "Estudiante registrado en el sistema SIENEP")
public class Estudiante extends Usuario {

    @Column(name = "ciudad", length = 50)
    @Schema(description = "Ciudad de residencia", example = "Montevideo")
    private String ciudad;

    @Column(name = "departamento", length = 20)
    @Schema(description = "Departamento de residencia", example = "Montevideo")
    private String departamento;

    @Column(name = "calle", length = 160)
    @Schema(description = "Calle de residencia", example = "Av. 18 de Julio")
    private String calle;

    @Column(name = "nro_puerta")
    @Schema(description = "Numero de puerta", example = "1234")
    private Integer nroPuerta;

    @Column(name = "inf_esta_salud", columnDefinition = "TEXT")
    @Schema(description = "Informacion de estado de salud - acceso restringido")
    private String informacionSalud;

    @Column(name = "sis_salud", length = 80)
    @Schema(description = "Sistema de salud", example = "ASSE")
    private String sistemaSalud;

    @Column(name = "fec_nacimiento", nullable = false)
    @Schema(description = "Fecha de nacimiento", example = "2000-05-15")
    private LocalDate fechaNacimiento;

    @Column(name = "motivo", columnDefinition = "TEXT")
    @Schema(description = "Motivo de derivacion al servicio psicopedagogico")
    private String motivoDerivacion;

    @Column(name = "foto", length = 255)
    @Schema(description = "URL o ruta de la foto del estudiante")
    private String foto;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime estudianteCreatedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime estudianteUpdatedAt;

    // =========================================================
    // RELACIONES
    // =========================================================

    @OneToMany(
            mappedBy = "estudiante",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    @Schema(description = "Telefonos del estudiante")
    private List<Telefono> telefonos = new ArrayList<>();

    /**
     * Grupos del estudiante via tabla intermedia estu_grupos.
     *
     * Los campos extra de estu_grupos (estado, created_at, updated_at)
     * son gestionados por triggers de BD. @ManyToMany los ignora — es
     * el alcance correcto para el modulo actual.
     */
    @ManyToMany(
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    @JoinTable(
            name = "estu_grupos",
            schema = "proyecto",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_grupo")
    )
    @Schema(description = "Grupos academicos del estudiante")
    private List<Grupo> grupos = new ArrayList<>();

    @OneToMany(
            mappedBy = "estudiante",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    @Schema(description = "Comentarios de seguimiento del estudiante")
    private List<ComentarioNormalEstudiante> comentarios = new ArrayList<>();

    @OneToMany(
            mappedBy = "estudiante",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    @Schema(description = "Observaciones confidenciales - acceso restringido a Direccion",
            hidden = true)
    private List<ComentarioConfidencialEstudiante> comentariosConfidenciales = new ArrayList<>();

    @OneToMany(
            mappedBy = "estudiante",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    @Schema(description = "Informes y documentos adjuntos del estudiante")
    private List<InformeAdjunto> informesAdjuntos = new ArrayList<>();

    // =========================================================
    // CONSTRUCTORES
    // =========================================================

    public Estudiante(String nombre, String apellido, String documento, PaisDocumento paisDocumento,
                      String contrasenia, String email, String ciudad, String departamento,
                      String calle, Integer nroPuerta, String informacionSalud, String sistemaSalud,
                      LocalDate fechaNacimiento, String motivoDerivacion, String foto,
                      List<Telefono> telefonos, List<Grupo> grupos) {
        super(nombre, apellido, documento, paisDocumento, contrasenia, email);
        this.estado = EstadoLogico.PENDIENTE_DE_ACTIVACION;
        this.ciudad = ciudad;
        this.departamento = departamento;
        this.calle = calle;
        this.nroPuerta = nroPuerta;
        this.informacionSalud = informacionSalud;
        this.sistemaSalud = sistemaSalud;

        this.fechaNacimiento = Objects.requireNonNull(fechaNacimiento, "fechaNacimiento NO puede ser null");

        int edad = Period.between(this.fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) {
            throw new IllegalArgumentException("El estudiante debe ser mayor de edad (minimo 18 anos).");
        }

        this.motivoDerivacion = motivoDerivacion;
        this.foto = foto;
        this.telefonos = telefonos != null ? telefonos : new ArrayList<>();
        this.grupos = grupos != null ? grupos : new ArrayList<>();
        this.comentarios = new ArrayList<>();
        this.comentariosConfidenciales = new ArrayList<>();
        this.informesAdjuntos = new ArrayList<>();
    }

    @EqualsAndHashCode.Include
    public Integer getIdForEquality() {
        return this.getIdUsuario();
    }
}