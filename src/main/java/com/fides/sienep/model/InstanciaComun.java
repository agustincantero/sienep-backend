package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "inst_comunes", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class InstanciaComun {

    @EmbeddedId
    private InstanciaComunId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("codInstancia")
    @JoinColumn(name = "cod_instancia")
    private Instancia instancia;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idCategoria")
    @JoinColumn(name = "id_categoria")
    private CategoriaInstancia categoria;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    public InstanciaComun(Instancia instancia, CategoriaInstancia categoria) {
        this.id = new InstanciaComunId(instancia.getCodInstancia(), categoria.getIdCategoria());
        this.instancia = instancia;
        this.categoria = categoria;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class InstanciaComunId implements Serializable {

        @Column(name = "cod_instancia")
        private int codInstancia;

        @Column(name = "id_categoria")
        private int idCategoria;

        public InstanciaComunId(int codInstancia, int idCategoria) {
            this.codInstancia = codInstancia;
            this.idCategoria = idCategoria;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InstanciaComunId)) return false;
            InstanciaComunId that = (InstanciaComunId) o;
            return codInstancia == that.codInstancia && idCategoria == that.idCategoria;
        }

        @Override
        public int hashCode() {
            return Objects.hash(codInstancia, idCategoria);
        }
    }
}