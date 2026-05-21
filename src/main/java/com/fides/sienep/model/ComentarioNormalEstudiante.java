package com.fides.sienep.model;

import java.time.LocalDateTime;

public class ComentarioNormalEstudiante extends ComentarioBase {
    private Estudiante estudiante;

    public ComentarioNormalEstudiante() {
        super();
    }

    public ComentarioNormalEstudiante(int idComentario, String comentario, Funcionario autor, Estudiante estudiante, EstadoLogico estado, LocalDateTime createdAt) {
        super(idComentario, comentario, autor);
        this.estudiante = estudiante;
        this.estado = estado;
        this.createdAt = createdAt;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    @Override
    public String toString() {
        return "ComentarioNormalEstudiante{" + "estudiante=" + (estudiante != null ? estudiante.getNombre() : "N/A") + ", contenido='" + contenido + '\'' + ", estado=" + estado + '}';
    }
}

