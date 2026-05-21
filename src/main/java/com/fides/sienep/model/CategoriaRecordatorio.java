package com.fides.sienep.model;

import java.time.LocalDateTime;

public class CategoriaRecordatorio {

    private int idCategoria;
    private String nomCategoria;
    private String descripcion;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CategoriaRecordatorio() {
    }

    public CategoriaRecordatorio(int idCategoria, String nomCategoria, String descripcion, EstadoLogico estado, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idCategoria = idCategoria;
        this.nomCategoria = nomCategoria;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public CategoriaRecordatorio(String nomCategoria) {
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return "Categoria{" + "idCategoria=" + idCategoria + ", nomCategoria='" + nomCategoria + '\'' + ", estado=" + estado + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + '}';
    }
}


