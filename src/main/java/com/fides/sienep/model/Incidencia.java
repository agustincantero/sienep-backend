package com.fides.sienep.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Incidencia extends Instancia {

    private String lugar;
    private List<Involucrado> involucrados;

    public Incidencia() {
        super();
        this.involucrados = new ArrayList<>();
    }

    public Incidencia(String idNegInstancia, String titulo, LocalDateTime fechaHora, int idEstudiante, int idFuncionario, EstadoLogico estado, String canal, String lugar) {
        super(idNegInstancia, titulo, fechaHora, idEstudiante, idFuncionario, estado, canal);
        this.lugar = lugar;
        this.involucrados = new ArrayList<>();
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public List<Involucrado> getInvolucrados() {
        return involucrados;
    }

    public void setInvolucrados(List<Involucrado> involucrados) {
        this.involucrados = involucrados;
    }


    @Override
    public String toString() {
        return "Incidencia{" + super.toString() + ", lugar='" + lugar + '\'' + ", involucrados=" + involucrados + '}';
    }
}

