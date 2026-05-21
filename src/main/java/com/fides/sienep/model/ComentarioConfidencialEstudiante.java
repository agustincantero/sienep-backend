package com.fides.sienep.model;

import java.time.LocalDateTime;

public class ComentarioConfidencialEstudiante extends ComentarioBase {
    private Estudiante estudiante;

    public ComentarioConfidencialEstudiante(Integer id, String contenido, Funcionario autor, Estudiante estudiante) {
        super(id, contenido, autor);
        this.estudiante = estudiante;
    }

    public ComentarioConfidencialEstudiante(String contenido, Funcionario autor, Estudiante estudiante) {
        super(contenido, autor);
        this.estudiante = estudiante;
    }

    public ComentarioConfidencialEstudiante(int id, String contenido, Funcionario autor, Estudiante estudiante, boolean estado, LocalDateTime fechaHora) {
        super(id, contenido, autor);
        this.estudiante = estudiante;
        this.estado = estado ? EstadoLogico.ACTIVO : EstadoLogico.INACTIVO;
        this.createdAt = fechaHora;
    }

    public ComentarioConfidencialEstudiante() {
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    @Override
    public String toString() {
        return "ComentarioConfidencialEstudiante{" + "estudiante=" + (estudiante != null ? estudiante.getNombre() : "N/A") + ", contenido='" + contenido + '\'' + ", estado=" + estado + '}';
    }
}

