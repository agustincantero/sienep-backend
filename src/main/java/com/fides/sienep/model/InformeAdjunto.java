package com.fides.sienep.model;

import java.time.LocalDateTime;

public class InformeAdjunto {
    private Integer idInforme;
    private final Estudiante estudiante;
    private final String urlArchivo;
    private final String ruta;
    private final String nombre;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InformeAdjunto(Estudiante estudiante, String urlArchivo, String ruta, String nombre) {
        this.estudiante = estudiante;
        this.urlArchivo = urlArchivo;
        this.ruta = ruta;
        this.nombre = nombre;
        this.estado = EstadoLogico.ACTIVO;
    }

    public InformeAdjunto(Integer idInforme, Estudiante estudiante, String urlArchivo, String ruta, String nombre, EstadoLogico estado, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idInforme = idInforme;
        this.estudiante = estudiante;
        this.urlArchivo = urlArchivo;
        this.ruta = ruta;
        this.nombre = nombre;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getIdInforme() {
        return idInforme;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public String getUrlArchivo() {
        return urlArchivo;
    }

    public String getRuta() {
        return ruta;
    }

    public String getNombre() {
        return nombre;
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

    public void setEstado(EstadoLogico estado) {
        this.estado = estado;
    }

    public void setIdInforme(Integer idInforme) {
        this.idInforme = idInforme;
    }

    @Override
    public String toString() {
        return "InformeAdj{" + "estudiante=" + (estudiante != null ? estudiante.getNombre() : "N/A") + ", nombre='" + nombre + '\'' + ", urlArchivo='" + urlArchivo + '\'' + ", ruta='" + ruta + '\'' + ", estado=" + estado + '}';
    }
}

