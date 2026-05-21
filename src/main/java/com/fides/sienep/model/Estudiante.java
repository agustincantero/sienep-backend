package com.fides.sienep.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Estudiante extends Usuario {
    private String ciudad;
    private String departamento;
    private String calle;
    private Integer nroPuerta;
    private String informacionSalud;
    private String sistemaSalud;
    private LocalDate fechaNacimiento;
    private String motivoDerivacion;
    private String foto;

    private List<Telefono> telefonos;
    private List<Grupo> grupos;
    private List<ComentarioNormalEstudiante> comentarios;
    private List<ComentarioConfidencialEstudiante> comentariosConfidenciales;
    private List<InformeAdjunto> informesAdjuntos;

    public Estudiante(String nombre, String apellido, String documento, PaisDocumento paisDocumento, String contrasenia, String email, String ciudad, String departamento, String calle, Integer nroPuerta, String informacionSalud, String sistemaSalud, LocalDate fechaNacimiento, String motivoDerivacion, String foto, List<Telefono> telefonos, List<Grupo> grupos) {
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
            throw new IllegalArgumentException("El estudiante debe ser mayor de edad (mínimo 18 años).");
        }

        this.motivoDerivacion = motivoDerivacion;
        this.foto = foto;

        this.telefonos = telefonos != null ? telefonos : new ArrayList<>();
        this.grupos = grupos != null ? grupos : new ArrayList<>();
        this.comentarios = new ArrayList<>();
        this.comentariosConfidenciales = new ArrayList<>();
        this.informesAdjuntos = new ArrayList<>();
    }

    public Estudiante(int idUsuario, String nombre, String apellido, String documento, PaisDocumento paisDocumento, String contrasenia, String email, EstadoLogico estado, LocalDateTime createdAt, LocalDateTime updatedAt, String ciudad, String departamento, String calle, Integer nroPuerta, String informacionSalud, String sistemaSalud, LocalDate fechaNacimiento, String motivoDerivacion, String foto, List<Telefono> telefonos, List<Grupo> grupos, List<ComentarioNormalEstudiante> comentarios, List<ComentarioConfidencialEstudiante> comentariosConfidenciales, List<InformeAdjunto> informesAdjuntos) {
        super(idUsuario, nombre, apellido, documento, paisDocumento, contrasenia, email, estado, createdAt, updatedAt);
        this.ciudad = ciudad;
        this.departamento = departamento;
        this.calle = calle;
        this.nroPuerta = nroPuerta;
        this.informacionSalud = informacionSalud;
        this.sistemaSalud = sistemaSalud;
        this.fechaNacimiento = Objects.requireNonNull(fechaNacimiento, "fechaNacimiento NO puede ser null");
        this.motivoDerivacion = motivoDerivacion;
        this.foto = foto;
        this.telefonos = telefonos;
        this.grupos = grupos;
        this.comentarios = comentarios;
        this.comentariosConfidenciales = comentariosConfidenciales;
        this.informesAdjuntos = informesAdjuntos;

        this.telefonos = telefonos != null ? telefonos : new ArrayList<>();
        this.grupos = grupos != null ? grupos : new ArrayList<>();
        this.comentarios = comentarios != null ? comentarios : new ArrayList<>();
        this.comentariosConfidenciales = comentariosConfidenciales != null ? comentariosConfidenciales : new ArrayList<>();
        this.informesAdjuntos = informesAdjuntos != null ? informesAdjuntos : new ArrayList<>();

    }

    public Estudiante() {
        super();
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getDepartamento() {
        return departamento;
    }

    public String getCalle() {
        return calle;
    }

    public Integer getNroPuerta() {
        return nroPuerta;
    }

    public String getInformacionSalud() {
        return informacionSalud;
    }

    public String getSistemaSalud() {
        return sistemaSalud;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getMotivoDerivacion() {
        return motivoDerivacion;
    }

    public String getFoto() {
        return foto;
    }

    public List<Telefono> getTelefonos() {
        return telefonos;
    }

    public List<Grupo> getGrupos() {
        return grupos;
    }

    public List<ComentarioNormalEstudiante> getComentarios() {
        return comentarios;
    }

    public List<ComentarioConfidencialEstudiante> getComentariosConfidenciales() {
        return comentariosConfidenciales;
    }

    public List<InformeAdjunto> getInformesAdjuntos() {
        return informesAdjuntos;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public void setNroPuerta(Integer nroPuerta) {
        this.nroPuerta = nroPuerta;
    }

    public void setInformacionSalud(String informacionSalud) {
        this.informacionSalud = informacionSalud;
    }

    public void setSistemaSalud(String sistemaSalud) {
        this.sistemaSalud = sistemaSalud;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public void setMotivoDerivacion(String motivoDerivacion) {
        this.motivoDerivacion = motivoDerivacion;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public void setTelefonos(List<Telefono> telefonos) {
        this.telefonos = telefonos;
    }

    public void setGrupos(List<Grupo> grupos) {
        this.grupos = grupos;
    }

    public void setComentarios(List<ComentarioNormalEstudiante> comentarios) {
        this.comentarios = comentarios;
    }

    public void setComentariosConfidenciales(List<ComentarioConfidencialEstudiante> comentariosConfidenciales) {
        this.comentariosConfidenciales = comentariosConfidenciales;
    }

    public void setInformesAdjuntos(List<InformeAdjunto> informesAdjuntos) {
        this.informesAdjuntos = informesAdjuntos;
    }


    @Override
    public String toString() {
        return "Estudiante{" + "idUsuario=" + idUsuario + ", nombre='" + nombre + '\'' + ", apellido='" + apellido + '\'' + ", documento='" + documento + '\'' + ", paisDocumento=" + paisDocumento + ", email='" + email + '\'' + ", estado=" + estado + ", fechaNacimiento=" + fechaNacimiento + ", ciudad='" + ciudad + '\'' + ", telefonos=" + telefonos + ", grupos=" + grupos + '}';
    }
}
