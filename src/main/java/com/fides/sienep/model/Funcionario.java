package com.fides.sienep.model;

import java.time.LocalDateTime;

public class Funcionario extends Usuario {
    private Rol rol;

    public Funcionario(String nombreUsuario, String apellidoUsuario, String documentoUsuario, PaisDocumento paisDocumento, String contrasenia, String email, Rol rol) {
        super(nombreUsuario, apellidoUsuario, documentoUsuario, paisDocumento, contrasenia, email);
        this.rol = rol;
        this.estado = EstadoLogico.PENDIENTE_DE_ACTIVACION;
    }

    public Funcionario(int idUsuario, String nombreUsuario, String apellidoUsuario, String documentoUsuario, PaisDocumento paisDocumento, String contrasenia, String email, EstadoLogico estado, LocalDateTime createdAt, LocalDateTime updatedAt, Rol rol) {
        super(idUsuario, nombreUsuario, apellidoUsuario, documentoUsuario, paisDocumento, contrasenia, email, estado, createdAt, updatedAt);
        this.rol = rol;
    }

    public Funcionario() {
        super();
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return "Funcionario{" + ", idUsuario=" + idUsuario + ", nombre='" + nombre + '\'' + ", apellido='" + apellido + '\'' + ", email='" + email + '\'' + ", documento='" + documento + '\'' + ", estado=" + estado + ", paisDocumento=" + paisDocumento + "rol= " + (rol != null ? rol.getNombre() : "N/A") + '}';
    }
}
