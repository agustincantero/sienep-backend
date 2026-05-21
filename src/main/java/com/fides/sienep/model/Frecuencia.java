package com.fides.sienep.model;

import java.time.LocalDateTime;

public class Frecuencia {

    private int idFrecuencia;
    private String descripcion;
    private TipoFrecuencia tipo;
    private Integer intDias;
    private DiaSemana diaSemana;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Frecuencia() {
    }

    public Frecuencia(int idFrecuencia, String descripcion, TipoFrecuencia tipo, Integer intDias, DiaSemana diaSemana, EstadoLogico estado, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idFrecuencia = idFrecuencia;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.intDias = intDias;
        this.diaSemana = diaSemana;
        this.estado = EstadoLogico.ACTIVO;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getIdFrecuencia() {
        return idFrecuencia;
    }

    public void setIdFrecuencia(int idFrecuencia) {
        this.idFrecuencia = idFrecuencia;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoFrecuencia getTipo() {
        return tipo;
    }

    public void setTipo(TipoFrecuencia tipo) {
        this.tipo = tipo;
    }

    public Integer getIntDias() {
        return intDias;
    }

    public void setIntDias(Integer intDias) {
        this.intDias = intDias;
    }

    public DiaSemana getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(DiaSemana diaSemana) {
        this.diaSemana = diaSemana;
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
        return "Frecuencia{" + "idFrecuencia=" + idFrecuencia + ", descripcion='" + descripcion + '\'' + ", tipo=" + tipo + ", intDias=" + intDias + ", diaSemana=" + diaSemana + ", estado=" + estado + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + '}';
    }
}

