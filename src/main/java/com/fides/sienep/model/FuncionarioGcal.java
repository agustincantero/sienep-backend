package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "funcionario_gcal", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class FuncionarioGcal {

    @Id
    @Column(name = "id_usuario")
    private int idUsuario;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_usuario")
    private Funcionario funcionario;

    @Column(name = "refresh_token", nullable = false, length = 512)
    private String refreshToken;

    @Column(name = "gcal_email", nullable = false, length = 100)
    private String gcalEmail;

    @Column(name = "autorizado", nullable = false)
    private boolean autorizado = true;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private LocalDateTime updatedAt;

    public FuncionarioGcal(Funcionario funcionario, String refreshToken, String gcalEmail) {
        this.funcionario = funcionario;
        this.refreshToken = refreshToken;
        this.gcalEmail = gcalEmail;
        this.autorizado = true;
    }
}