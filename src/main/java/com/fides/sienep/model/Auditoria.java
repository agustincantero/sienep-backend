package com.fides.sienep.model;

import java.time.LocalDateTime;

public class Auditoria {
    private int id;
    private String entidad;
    private AccionAuditoria accion;
    private Funcionario autor;
    private LocalDateTime fechaHora;
    private String detalle;

    public Auditoria() {
    }

    public Auditoria(String entidad, AccionAuditoria accion, Funcionario autor, String detalle) {
        this.entidad = entidad;
        this.accion = accion;
        this.autor = autor;
        this.detalle = detalle;
        this.fechaHora = LocalDateTime.now();
    }

    public String getEntidad() {
        return entidad;
    }

    public int getId() {
        return id;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public Funcionario getAutor() {
        return autor;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setAccion(AccionAuditoria accion) {
        this.accion = accion;
    }

    public void setAutor(Funcionario autor) {
        this.autor = autor;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}

