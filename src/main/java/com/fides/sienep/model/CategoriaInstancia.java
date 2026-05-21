package com.fides.sienep.model;

import java.time.LocalDateTime;

public class CategoriaInstancia {

    private int idCategoria;
    private String nomCategoria;
    private String descripcion;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CategoriaInstancia() {
    }

    public CategoriaInstancia(int idCategoria, String nomCategoria, String descripcion, EstadoLogico estado) {
        this.idCategoria = idCategoria;
        this.nomCategoria = nomCategoria;
        this.estado = estado;
    }

    public CategoriaInstancia(String nomCategoria) {
        this.nomCategoria = nomCategoria;
        this.estado = EstadoLogico.ACTIVO;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNomCategoria() {
        return nomCategoria;
    }

    public void setNomCategoria(String nomCategoria) {
        this.nomCategoria = nomCategoria;
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
        return "Categoria{" + "idCategoria=" + idCategoria + ", nomCategoria='" + nomCategoria + '\'' + ", estado=" + estado + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + '}';
    }
}


