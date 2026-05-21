package com.fides.sienep.model;

import java.time.LocalDateTime;


public class Telefono {
    private Usuario usuario;
    private String numero;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Telefono(Usuario usuario, String numero) {
        this.usuario = usuario;
        this.numero = numero;
        this.estado = EstadoLogico.ACTIVO;
    }

    public Telefono(Usuario usuario, String numero, EstadoLogico estado, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.usuario = usuario;
        this.numero = numero;
        this.estado = EstadoLogico.ACTIVO;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public EstadoLogico getEstado() {
        return estado;
    }

    public void setEstado(EstadoLogico estado) {
        this.estado = estado;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Telefono{" + "usuario=" + (usuario != null ? usuario.getNombre() : "N/A") + ", numero='" + numero + '\'' + ", estado=" + estado + '}';
    }
}

