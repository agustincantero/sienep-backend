package com.fides.sienep.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Carrera {

    private int idCarrera;
    private String nombreCarrera;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ITR> itrs;

    public Carrera(String nombreCarrera) {
        this.nombreCarrera = nombreCarrera;
        this.estado = EstadoLogico.ACTIVO;
        this.itrs = new ArrayList<ITR>();
    }

    public Carrera(int idCarrera, String nombreCarrera, EstadoLogico activo, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idCarrera = idCarrera;
        this.nombreCarrera = nombreCarrera;
        this.estado = activo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.itrs = new ArrayList<>();
    }

    public int getIdCarrera() {
        return idCarrera;
    }

    public void setIdCarrera(int idCarrera) {
        this.idCarrera = idCarrera;
    }

    public String getNombreCarrera() {
        return nombreCarrera;
    }

    public void setNombreCarrera(String nombreCarrera) {
        this.nombreCarrera = nombreCarrera;
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

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ITR> getItrs() {
        return itrs;
    }

    public void agregarItr(ITR itr) {
        if (itr != null && !itrs.contains(itr)) {
            itrs.add(itr);
        }
    }

    @Override
    public String toString() {
        return "Carrera{" + "idCarrera=" + idCarrera + ", nombreCarrera='" + nombreCarrera + '\'' + ", estado=" + estado + '}';
    }
}
