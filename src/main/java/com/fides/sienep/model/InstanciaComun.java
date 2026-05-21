package com.fides.sienep.model;

import java.time.LocalDateTime;

public class InstanciaComun extends Instancia {

    private CategoriaInstancia categoria;

    public InstanciaComun() {
        super();
    }

    public InstanciaComun(String idNegInstancia, String titulo, LocalDateTime fechaHora, int idEstudiante, int idFuncionario, EstadoLogico estado, String canal, CategoriaInstancia categoria) {
        super(idNegInstancia, titulo, fechaHora, idEstudiante, idFuncionario, estado, canal);
        this.categoria = categoria;
    }

    public InstanciaComun(String idNegInstancia, String titulo, LocalDateTime fecHora, int idEstudiante, int idFuncionario, EstadoLogico estado, String canal, CategoriaInstancia categoria, LocalDateTime createdAt, LocalDateTime localDateTime) {
        this.idNegInstancia = idNegInstancia;
        this.titulo = titulo;
        this.fechaHora = fecHora;
        this.idEstudiante = idEstudiante;
        this.idFuncionario = idFuncionario;
        this.estado = estado;
        this.canal = canal;
        this.createdAt = createdAt;
        this.updatedAt = localDateTime;
    }

    public CategoriaInstancia getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaInstancia categoria) {
        this.categoria = categoria;
    }

    @Override
    public String toString() {
        return super.toString() + '\n' + "Categoria= " + (categoria != null ? categoria.getNomCategoria() : "sin categoría") + '}' + '\n';
    }
}

