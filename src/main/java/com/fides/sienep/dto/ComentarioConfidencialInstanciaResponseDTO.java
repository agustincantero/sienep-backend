package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ComentarioConfidencialInstanciaResponseDTO {

    private Integer idComConfidencial;
    private Integer codInstancia;
    // null si el comentario está ELIMINADO: se lista como marca en el seguimiento, sin su texto.
    private String contenido;
    // true si el contenido se modificó después de creado; la fecha de la edición es updatedAt.
    private boolean editado;
    private Integer idAutor;
    private String nombreAutor;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    // Fecha de la última edición o, si está ELIMINADO, de la eliminación.
    private LocalDateTime updatedAt;
}