package com.fides.sienep.model;

import java.time.LocalDateTime;

public abstract class ComentarioBase {
    protected Integer id;
    protected String contenido;
    protected Funcionario autor;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    protected EstadoLogico estado;

    public ComentarioBase(Integer id, String contenido, Funcionario autor) {
        this.id = id;
        this.contenido = contenido;
        this.autor = autor;
        this.estado = EstadoLogico.ACTIVO;
    }

    public ComentarioBase(String contenido, Funcionario autor) {
        this.contenido = contenido;
        this.autor = autor;
        this.estado = EstadoLogico.ACTIVO;
    }

    public ComentarioBase() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Funcionario getAutor() {
        return autor;
    }

    public void setAutor(Funcionario autor) {
        this.autor = autor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public EstadoLogico getEstado() {
        return estado;
    }

    public void setEstado(EstadoLogico estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "ComentarioBase{" + "id=" + id + ", contenido='" + contenido + '\'' + ", autor=" + (autor != null ? autor.getNombre() : "N/A") + ", estado=" + estado + '}';
    }
}
