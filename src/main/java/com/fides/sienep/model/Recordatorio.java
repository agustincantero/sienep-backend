package com.fides.sienep.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Recordatorio {

    private int codRecordatorio;
    private String idNegRecordatorio;
    private String titulo;
    private TipoRecordatorio tipo;
    private LocalDateTime fechaHora;
    private int codInstancia;
    private int idCategoria;
    private int responsable;
    private Frecuencia frecuencia;
    private EstadoRecordatorio estadoOperativo;
    private LocalDate fechaFin;
    private int nroOcurrencias;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Recordatorio() {
    }

    public Recordatorio(int codRecordatorio, String idNegRecordatorio, String titulo, TipoRecordatorio tipo, LocalDateTime fechaHora, int codInstancia, Frecuencia frecuencia, EstadoRecordatorio estadoOperativo, LocalDate fechaFin, int nroOcurrencias, EstadoLogico estado, LocalDateTime createdAt, LocalDateTime updatedAt, int responsable, int idCategoria) {
        this.codRecordatorio = codRecordatorio;
        this.idNegRecordatorio = idNegRecordatorio;
        this.titulo = titulo;
        this.tipo = tipo;
        this.fechaHora = fechaHora;
        this.codInstancia = codInstancia;
        this.frecuencia = frecuencia;
        this.estadoOperativo = EstadoRecordatorio.FUTURO;
        this.fechaFin = fechaFin;
        this.nroOcurrencias = nroOcurrencias;
        this.estado = EstadoLogico.ACTIVO;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.responsable = responsable;
        this.idCategoria = idCategoria;
    }

    public int getCodRecordatorio() {
        return codRecordatorio;
    }

    public void setCodRecordatorio(int codRecordatorio) {
        this.codRecordatorio = codRecordatorio;
    }

    public String getIdNegRecordatorio() {
        return idNegRecordatorio;
    }

    public void setIdNegRecordatorio(String idNegRecordatorio) {
        this.idNegRecordatorio = idNegRecordatorio;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public TipoRecordatorio getTipo() {
        return tipo;
    }

    public void setTipo(TipoRecordatorio tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public int getCodInstancia() {
        return codInstancia;
    }

    public void setCodInstancia(int codInstancia) {
        this.codInstancia = codInstancia;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public int getResponsable() {
        return responsable;
    }

    public void setResponsable(int responsable) {
        this.responsable = responsable;
    }

    public Frecuencia getFrecuencia() {
        return frecuencia;
    }

    public void setFrecuencia(Frecuencia frecuencia) {
        this.frecuencia = frecuencia;
    }

    public EstadoRecordatorio getEstadoOperativo() {
        return estadoOperativo;
    }

    public void setEstadoOperativo(EstadoRecordatorio estadoOperativo) {
        this.estadoOperativo = estadoOperativo;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public int getNroOcurrencias() {
        return nroOcurrencias;
    }

    public void setNroOcurrencias(int nroOcurrencias) {
        this.nroOcurrencias = nroOcurrencias;
    }

    public EstadoLogico getEstadoLogico() {
        return estado;
    }

    public void setEstadoLogico(EstadoLogico estado) {
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
        return "Recordatorio{" + "codRecordatorio=" + codRecordatorio + ", idNegRecordatorio='" + idNegRecordatorio + '\'' + ", titulo='" + titulo + '\'' + ", tipo=" + tipo + ", fechaHora=" + fechaHora + ", codInstancia=" + codInstancia + ", frecuencia=" + (frecuencia != null ? frecuencia.getDescripcion() : "sin frecuencia") + ", estadoOperativo=" + estadoOperativo + ", fechaFin=" + fechaFin + ", nroOcurrencias=" + nroOcurrencias + ", estado=" + estado + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + '}';
    }
}

