package com.fides.sienep.model;

import java.time.LocalDateTime;

public class ComentarioConfidencialInstancia extends ComentarioBase {
    private Instancia instancia;


    public ComentarioConfidencialInstancia(String contenido, Funcionario autor, Instancia instancia) {
        super(contenido, autor);
        this.instancia = instancia;
    }

    public ComentarioConfidencialInstancia(int idComConfidencial, String comentario, Funcionario autor, Instancia instancia, EstadoLogico estado, LocalDateTime fecHora) {
        super(idComConfidencial, comentario, autor);
        this.instancia = instancia;
        this.estado = estado;
        this.createdAt = fecHora;
    }

    public Instancia getInstancia() {
        return instancia;
    }

    public void setInstancia(Instancia instancia) {
        this.instancia = instancia;
    }

    @Override
    public String toString() {
        return "ComentarioConfidencialInstancia{" + "instancia=" + (instancia != null ? instancia.getIdNegInstancia() : "N/A") + ", contenido='" + contenido + '\'' + ", estado=" + estado + '}';
    }
}

