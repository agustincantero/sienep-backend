package com.fides.sienep.model;

public class Permiso {
    private String nombre;
    private String descripcion;
    private int idPermiso;
    private EstadoLogico estado;

    public Permiso(String nombre, int idPermiso, String descripcion) {
        this.nombre = nombre;
        this.idPermiso = idPermiso;
        this.descripcion = descripcion;
        this.estado = EstadoLogico.ACTIVO;
    }

    public Permiso(String nombre, int idPermiso, String descripcion, EstadoLogico estado) {
        this.nombre = nombre;
        this.idPermiso = idPermiso;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public Permiso(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = EstadoLogico.ACTIVO;
    }

    public Permiso() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getIdPermiso() {
        return idPermiso;
    }

    public void setIdPermiso(int idPermiso) {
        this.idPermiso = idPermiso;
    }

    public EstadoLogico getEstado() {
        return estado;
    }

    public void setEstado(EstadoLogico estado) {
        this.estado = estado;
    }

}