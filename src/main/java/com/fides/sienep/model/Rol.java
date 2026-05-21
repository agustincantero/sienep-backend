package com.fides.sienep.model;

import java.util.ArrayList;
import java.util.List;

public class Rol {
    private String nombre;
    private int idRol;
    private List<Permiso> permisos;
    private EstadoLogico estado;

    public Rol(String nombre, int idRol, List<Permiso> permisos, EstadoLogico estado) {
        this.nombre = nombre;
        this.idRol = idRol;
        this.permisos = permisos != null ? permisos : new ArrayList<>();
        this.estado = estado;
    }

    public Rol(String nombre) {
        this.nombre = nombre;
        this.estado = EstadoLogico.ACTIVO;
        this.permisos = new ArrayList<>();
    }

    public Rol() {
    }


    public boolean tienePermiso(String nombrePermiso) {
        if (permisos == null) return false;
        for (Permiso p : permisos) {
            if (p.getNombre().equals(nombrePermiso)) return true;
        }
        return false;
    }

    public void agregarPermiso(Permiso permiso) {
        if (permisos == null) permisos = new ArrayList<>();
        permisos.add(permiso);
    }

    public List<Permiso> getPermisos() {
        return permisos;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public EstadoLogico getEstado() {
        return estado;
    }

    public void setEstado(EstadoLogico estado) {
        this.estado = estado;
    }

    public void setPermisos(List<Permiso> permisos) {
        this.permisos = permisos;
    }
}
