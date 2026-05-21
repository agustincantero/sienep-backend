package com.fides.sienep.model;

import java.time.LocalDateTime;

public class Grupo {
    private int idGrupo;
    private int idCarrera;
    private String nombre;
    private int generacion;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Grupo(Integer idCarrera, String nombre, int generacion) {
        this.idCarrera = idCarrera;
        this.nombre = nombre;
        this.generacion = generacion;
        this.estado = EstadoLogico.ACTIVO;
    }

    public Grupo(int idGrupo, int idCarrera, String nombre, int generacion, EstadoLogico estado, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idGrupo = idGrupo;
        this.idCarrera = idCarrera;
        this.nombre = nombre;
        this.generacion = generacion;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Grupo(String nombre) {
        this.nombre = nombre;
    }

    public Integer getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(int idGrupo) {
        this.idGrupo = idGrupo;
    }

    public Integer getIdCarrera() {
        return idCarrera;
    }

    public void setIdCarrera(int idCarrera) {
        this.idCarrera = idCarrera;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getGeneracion() {
        return generacion;
    }

    public void setGeneracion(int generacion) {
        this.generacion = generacion;
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
        return "Grupo{" + "idGrupo=" + idGrupo + ", idCarrera=" + idCarrera + ", nombre='" + nombre + '\'' + ", generacion='" + generacion + '\'' + ", estado=" + estado + '}';
    }
}
