package com.fides.sienep.model;

import com.fides.sienep.model.Rol;
import com.fides.sienep.model.Usuario;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "funcionarios", schema = "proyecto")
public class Funcionario {
    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;
    @OneToOne
    @MapsId
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;
    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    public Funcionario() {
    }

// getters y setters


    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    // Contructores

    public Funcionario(Integer idUsuario, LocalDateTime updatedAt, LocalDateTime createdAt, Rol rol, Usuario usuario) {
        this.idUsuario = idUsuario;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
        this.rol = rol;
        this.usuario = usuario;
    }

    @EqualsAndHashCode.Include
    private Integer getIdForEquality() {
        return this.getIdUsuario();
    }
}