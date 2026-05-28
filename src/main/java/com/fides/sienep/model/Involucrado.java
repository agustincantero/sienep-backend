package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "involucrados", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class Involucrado {

    @EmbeddedId
    private InvolucradoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("codInstancia")
    @JoinColumn(name = "cod_instancia")
    private Instancia instancia;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false, columnDefinition = "proyecto.estado_logico")
    private EstadoLogico estado;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    public Involucrado(Instancia instancia, String nombre) {
        this.id = new InvolucradoId(instancia.getCodInstancia(), nombre);
        this.instancia = instancia;
        this.estado = EstadoLogico.ACTIVO;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class InvolucradoId implements Serializable {

        @Column(name = "cod_instancia")
        private int codInstancia;

        @Column(name = "nom_involucrado", length = 30)
        private String nomInvolucrado;

        public InvolucradoId(int codInstancia, String nomInvolucrado) {
            this.codInstancia = codInstancia;
            this.nomInvolucrado = nomInvolucrado;
        }

        // Obligatorio en clases @Embeddable usadas como PK compuesta.
        // Hibernate necesita comparar instancias de esta clave para identificar
        // entidades en su caché interna y evitar duplicados o búsquedas fallidas.
        // Dos InvolucradoId son iguales si tienen el mismo codInstancia Y nomInvolucrado.
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InvolucradoId)) return false;
            InvolucradoId that = (InvolucradoId) o;
            return codInstancia == that.codInstancia &&
                    Objects.equals(nomInvolucrado, that.nomInvolucrado);
        }

        // Obligatorio junto con equals: si dos objetos son iguales según equals,
        // deben producir el mismo hashCode. Hibernate lo usa internamente
        // en colecciones como HashMap y HashSet para localizar entidades por su PK.
        @Override
        public int hashCode() {
            return Objects.hash(codInstancia, nomInvolucrado);
        }
    }
}