package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "token_restablecimiento", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class TokenRestablecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token")
    private int idToken;

    @Column(name = "id_usuario", nullable = false)
    private int idUsuario;

    @Column(name = "token_hash", nullable = false, length = 255, unique = true)
    private String tokenHash;

    @Column(name = "fec_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "usado", nullable = false)
    private boolean usado = false;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    public TokenRestablecimiento(int idUsuario, String tokenHash, LocalDateTime fechaExpiracion) {
        this.idUsuario = idUsuario;
        this.tokenHash = tokenHash;
        this.fechaExpiracion = fechaExpiracion;
    }
}
