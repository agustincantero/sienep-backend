package com.fides.sienep.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fides.sienep.model.TipoAccion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Data Transfer Object que representa un registro de auditoría del sistema")
public class AuditoriaResponseDTO {

    @Schema(description = "ID único del registro de auditoría", example = "10")
    private int idAuditoria;

    @Schema(description = "Nombre de la tabla/entidad que fue modificada", example = "USUARIOS")
    private String entidad;

    @Schema(description = "Acción realizada", example = "UPDATE")
    private TipoAccion accion;

    @Schema(description = "ID del usuario de la aplicación que realizó la acción. Nulo si fue un DBA directamente en BD", example = "5")
    private Integer idAutor;

    @Schema(description = "Nombre completo del usuario que realizó la acción", example = "Agustín Cantero")
    private String nombreAutor;

    @Schema(description = "Documento del usuario que realizó la acción, sin el país (ver Usuario.paisDocumento). Nulo si fue un DBA directamente en BD", example = "12345678")
    private String documentoAutor;

    @Schema(description = "Usuario del motor de Base de Datos que ejecutó la consulta", example = "sienep_app")
    private String usuarioBd;

    @Schema(description = "Fecha y hora exacta en que ocurrió el evento")
    private LocalDateTime fechaHora;

    @Schema(description = "Dirección IP desde donde se realizó la petición", example = "192.168.1.55")
    private String ip;

    @JsonRawValue
    @Schema(description = "Estado de la entidad ANTES del cambio (en formato JSON)")
    private String valorAnterior;

    @JsonRawValue
    @Schema(description = "Estado de la entidad DESPUÉS del cambio (en formato JSON)")
    private String valorNuevo;
}
