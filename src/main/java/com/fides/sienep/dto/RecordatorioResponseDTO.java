package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.EstadoRecordatorio;
import com.fides.sienep.model.TipoRecordatorio;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RecordatorioResponseDTO {

    private int codRecordatorio;
    private String idNegRecordatorio;
    private String titulo;
    private TipoRecordatorio tipo;
    private LocalDateTime fechaHora;
    private int codInstancia;
    private String tituloInstancia;
    private int idCategoria;
    private String nombreCategoria;
    private int idResponsable;
    private String nombreResponsable;
    private Integer idFrecuencia;
    private String descripcionFrecuencia;
    private LocalDate fechaFin;
    private Integer nroOcurrencias;
    private EstadoRecordatorio estadoOperativo;
    private EstadoLogico estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}