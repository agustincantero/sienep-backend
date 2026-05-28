package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "estudiantes", schema = "proyecto")
@PrimaryKeyJoinColumn(name = "id_usuario")
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = false, exclude = {"telefonos", "grupos", "comentarios", "comentariosConfidenciales", "informesAdjuntos"})
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Estudiante extends Usuario {

    @Column(name = "ciudad", length = 50)
    private String ciudad;

    @Column(name = "departamento", length = 20)
    private String departamento;

    @Column(name = "calle", length = 160)
    private String calle;

    @Column(name = "nro_puerta")
    private Integer nroPuerta;

    @Column(name = "inf_esta_salud", columnDefinition = "TEXT")
    private String informacionSalud;

    @Column(name = "sis_salud", length = 80)
    private String sistemaSalud;

    @Column(name = "fec_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivoDerivacion;

    @Column(name = "foto", length = 255)
    private String foto;

    // Los timestamps de la tabla estudiantes los gestiona el trigger de BD.
    // insertable = false, updatable = false: JPA nunca escribe estas columnas,
    // el trigger y el DEFAULT de PostgreSQL son los únicos que las modifican.
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime estudianteCreatedAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime estudianteUpdatedAt;

    @OneToMany(mappedBy = "estudiante", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<Telefono> telefonos = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(
            name = "estu_grupos", schema = "proyecto",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_grupo")
    )
    private List<Grupo> grupos = new ArrayList<>();

    @OneToMany(mappedBy = "estudiante", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<ComentarioNormalEstudiante> comentarios = new ArrayList<>();

    @OneToMany(mappedBy = "estudiante", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<ComentarioConfidencialEstudiante> comentariosConfidenciales = new ArrayList<>();

    @OneToMany(mappedBy = "estudiante", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<InformeAdjunto> informesAdjuntos = new ArrayList<>();

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
        this.fechaNacimiento = Objects.requireNonNull(fechaNacimiento, "fechaNacimiento no puede ser null");
        int edad = Period.between(this.fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) throw new IllegalArgumentException("El estudiante debe ser mayor de edad.");
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