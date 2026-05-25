package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios", schema = "proyecto")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public abstract class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    protected int idUsuario;

    @Column(nullable = false, length = 50)
    protected String nombre;

    @Column(nullable = false, length = 50)
    protected String apellido;

    @Column(nullable = false, length = 15)
    protected String documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "pais_documento", nullable = false)
    protected PaisDocumento paisDocumento;

    @Column(nullable = false, length = 255)
    protected String contrasenia;

    @Column(nullable = false, length = 50, unique = true, columnDefinition = "dom_email")
    protected String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    protected EstadoLogico estado;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    protected LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    protected LocalDateTime updatedAt;

    public Usuario(String nombre, String apellido, String documento, PaisDocumento paisDocumento, String contrasenia, String email) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.documento = documento;
        this.paisDocumento = paisDocumento;
        this.contrasenia = contrasenia;
        this.email = email;
        this.estado = EstadoLogico.PENDIENTE_DE_ACTIVACION;
    }

    public Usuario(int idUsuario, String nombre, String apellido, String documento, PaisDocumento paisDocumento, String contrasenia, String email, EstadoLogico estado, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.apellido = apellido;
        this.documento = documento;
        this.paisDocumento = paisDocumento;
        this.contrasenia = contrasenia;
        this.email = email;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
