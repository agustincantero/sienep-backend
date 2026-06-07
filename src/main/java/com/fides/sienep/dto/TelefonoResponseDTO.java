package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class TelefonoResponseDTO {
    private Integer idUsuario;
    private String numero;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
