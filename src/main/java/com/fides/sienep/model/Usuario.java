package com.fides.sienep.model;

import java.time.LocalDateTime;

public abstract class Usuario {
    protected int idUsuario;
    protected String nombre;
    protected String apellido;
    protected String documento;
    protected PaisDocumento paisDocumento;
    protected String contrasenia;
    protected String email;
    protected EstadoLogico estado;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    public Usuario(String nombre, String apellido, String documento, PaisDocumento paisDocumento, String contrasenia, String email) {

        this.nombre = nombre;
        this.apellido = apellido;
        this.documento = documento;
        this.paisDocumento = paisDocumento;
        this.contrasenia = contrasenia;
        this.email = email;
        this.estado = EstadoLogico.PENDIENTE_DE_ACTIVACION;
    }

    public Usuario(int idUsuario, String nombre, String apellido, String documento, PaisDocumento paisDocumento, String contrasenia, String email, EstadoLogico estado, LocalDateTime createdAt, LocalDateTime updatedAt) {

        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.documento = documento;
        this.paisDocumento = paisDocumento;
        this.contrasenia = contrasenia;
        this.email = email;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Usuario() {
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getDocumento() {
        return documento;
    }

    public PaisDocumento getPaisDocumento() {
        return paisDocumento;
    }

    public String getContrasenia() {
        return contrasenia;
    }

    public String getEmail() {
        return email;
    }

    public EstadoLogico getEstado() {
        return estado;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public void setPaisDocumento(PaisDocumento paisDocumento) {
        this.paisDocumento = paisDocumento;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }

    protected void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setEstado(EstadoLogico estado) {
        this.estado = estado;
    }


    public void setIdUsuario(int anInt) {
        this.idUsuario = anInt;
    }
}
