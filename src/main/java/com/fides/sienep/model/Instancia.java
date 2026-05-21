package com.fides.sienep.model;

import java.time.LocalDateTime;

public abstract class Instancia {
    protected int codInstancia;
    protected String idNegInstancia;
    protected String titulo;
    protected LocalDateTime fechaHora;
    protected int idEstudiante;
    protected int idFuncionario;
    protected EstadoLogico estado;
    protected String canal;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    protected String nombreEstudiante;

    protected Instancia(String titulo, LocalDateTime fechaHora, int idEstudiante, int idFuncionario, String canal) {
        this.titulo = titulo;
        this.fechaHora = fechaHora;
        this.idEstudiante = idEstudiante;
        this.idFuncionario = idFuncionario;
        this.estado = EstadoLogico.ACTIVO; // valor por defecto
        this.canal = canal;
    }

    protected Instancia(String titulo, LocalDateTime fechaHora, int idEstudiante, int idFuncionario, String canal, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.titulo = titulo;
        this.fechaHora = fechaHora;
        this.idEstudiante = idEstudiante;
        this.idFuncionario = idFuncionario;
        this.estado = EstadoLogico.ACTIVO;
        this.canal = canal;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    protected Instancia() {
        this.estado = EstadoLogico.ACTIVO;
    }

    protected Instancia(String idNegInstancia, String titulo, LocalDateTime fechaHora, int idEstudiante, int idFuncionario, EstadoLogico estado, String canal) {
        this.idNegInstancia = idNegInstancia;
        this.titulo = titulo;
        this.fechaHora = fechaHora;
        this.idEstudiante = idEstudiante;
        this.idFuncionario = idFuncionario;
        this.estado = estado;
        this.canal = canal;
    }

    public int getCodInstancia() {
        return codInstancia;
    }

    public void setCodInstancia(int codInstancia) {
        this.codInstancia = codInstancia;
    }

    public String getIdNegInstancia() {
        return idNegInstancia;
    }

    public void setIdNegInstancia(String idNegInstancia) {
        this.idNegInstancia = idNegInstancia;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public int getIdEstudiante() {
        return idEstudiante;
    }

    public void setIdEstudiante(int idEstudiante) {
        this.idEstudiante = idEstudiante;
    }

    public int getIdFuncionario() {
        return idFuncionario;
    }

    public void setIdFuncionario(int idFuncionario) {
        this.idFuncionario = idFuncionario;
    }

    public EstadoLogico getEstado() {
        return estado;
    }

    public void setEstado(EstadoLogico estado) {
        this.estado = estado;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
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

    public String getNombreEstudiante() {
        return nombreEstudiante;
    }

    public void setNombreEstudiante(String nombreEstudiante) {
        this.nombreEstudiante = nombreEstudiante;
    }

    @Override
    public String toString() {
        return "Instancia { " + '\n' + "codInstancia=" + codInstancia + '\n' + ", idNegInstancia='" + idNegInstancia + '\n' + ", titulo='" + titulo + '\n' + ", fechaHora=" + fechaHora + '\n' + ", idEstudiante=" + idEstudiante + '\n' + ", idFuncionario=" + idFuncionario + '\n' + ", estado=" + estado + '\n' + ", canal='" + canal + '\n' + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + '}';
    }
}

