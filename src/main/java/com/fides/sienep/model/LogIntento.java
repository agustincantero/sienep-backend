package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_intentos", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class LogIntento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_login")
    private int idLogin;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "fue_exitoso", nullable = false)
    private boolean fueExitoso;

    @Column(name = "fec_hora", nullable = false, updatable = false, insertable = false)
    private LocalDateTime fechaHora;

    public LogIntento(Integer idUsuario, boolean fueExitoso) {
        this.idUsuario = idUsuario;
        this.fueExitoso = fueExitoso;
    }
}