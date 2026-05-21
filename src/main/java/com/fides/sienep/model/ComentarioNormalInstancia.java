package com.fides.sienep.model;

import java.time.LocalDateTime;

public class ComentarioNormalInstancia extends ComentarioBase {
    private Instancia instancia;

    public ComentarioNormalInstancia(String contenido, Funcionario autor, Instancia instancia) {
        super(contenido, autor);
        this.instancia = instancia;
    }

    public ComentarioNormalInstancia(Integer idComentario, String comentario, Funcionario autor, Instancia instancia, EstadoLogico estado, LocalDateTime fecHora) {
        super(idComentario, comentario, autor);
        this.instancia = instancia;
        this.estado = estado;
        this.createdAt = fecHora;
    }

    public ComentarioNormalInstancia(int idComentario, String comentario, Funcionario autor, Instancia instancia, EstadoLogico estado, LocalDateTime fecHora) {
        super(idComentario, comentario, autor);
        this.instancia = instancia;
        this.estado = estado;
    }


    public Instancia getInstancia() {
        return instancia;
    }

    public void setInstancia(Instancia instancia) {
        this.instancia = instancia;
    }

    @Override
    public String toString() {
        return "ComentarioNormalInstancia{" + "instancia=" + (instancia != null ? instancia.getIdNegInstancia() : "N/A") + ", contenido='" + contenido + '\'' + ", estado=" + estado + '}';
    }
}

