package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            insertable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false,
            insertable = false
    )
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