package com.fides.sienep.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class InstanciaComunResponseDTO {

    private int codInstancia;
    private String tituloInstancia;
    private int idCategoria;
    private String nombreCategoria;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}