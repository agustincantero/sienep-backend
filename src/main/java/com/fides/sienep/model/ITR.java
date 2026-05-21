package com.fides.sienep.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ITR {

    private int idItr;
    private String nombreItr;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Carrera> carreras;

    public ITR(String nombreItr) {
        this.nombreItr = nombreItr;
        this.estado = EstadoLogico.ACTIVO;
        this.carreras = new ArrayList<>();
    }

    public ITR(int idItr, String nombreItr, EstadoLogico estado, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idItr = idItr;
        this.nombreItr = nombreItr;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.carreras = new ArrayList<>();
    }

    public int getIdItr() {
        return idItr;
    }

    public void setIdItr(int idItr) {
        this.idItr = idItr;
    }

    public String getNombreItr() {
        return nombreItr;
    }

    public void setNombreItr(String nombreItr) {
        this.nombreItr = nombreItr;
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

    public List<Carrera> getCarreras() {
        return carreras;
    }

    public void agregarCarrera(Carrera carrera) {
        if (carrera != null && !carreras.contains(carrera)) {
            carreras.add(carrera);
        }
    }

    @Override
    public String toString() {
        return "Itr{" + "idItr=" + idItr + ", nombreItr='" + nombreItr + '\'' + ", estado=" + estado + '}';
    }
}
