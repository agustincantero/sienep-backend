package com.fides.sienep.model;

import java.time.LocalDateTime;

public class Involucrado {

    private int codInstancia;
    private String nombre;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Involucrado() {
    }

    public Involucrado(int codInstancia, String nombre) {
        this.codInstancia = codInstancia;
        this.nombre = nombre;
        this.estado = EstadoLogico.ACTIVO;
    }

    public int getCodInstancia() {
        return codInstancia;
    }

    public void setCodInstancia(int codInstancia) {
        this.codInstancia = codInstancia;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }


    @Override
    public String toString() {
        return "Involucrado{" + "codInstancia=" + codInstancia + ", nombre='" + nombre + '\'' + ", estado=" + estado + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + '}';
    }
}

