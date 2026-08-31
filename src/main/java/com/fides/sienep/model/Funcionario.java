package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "funcionarios", schema = "proyecto")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Funcionario extends Usuario {

    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    // Ver Carrera.java para el porqué de @CreationTimestamp/@UpdateTimestamp acá.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Funcionario(
            String nombre,
            String apellido,
            String documento,
            PaisDocumento paisDocumento,
            String contrasenia,
            String email,
            Rol rol
    ) {

        super(
                nombre,
                apellido,
                documento,
                paisDocumento,
                contrasenia,
                email
        );

        this.rol = rol;
    }
}