package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoSolicitud;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Schema(description = "Solicitud de instancia de un estudiante")
public class SolicitudInstanciaResponseDTO {

    private Integer idSolicitud;
    private Integer idEstudiante;
    private String nombreEstudiante;
    private String motivo;
    private EstadoSolicitud estado;

    @Schema(description = "Mensaje del funcionario al resolverla (obligatorio si fue RECHAZADA)")
    private String respuesta;

    @Schema(description = "Quien la atendió o la rechazó; null mientras está PENDIENTE")
    private Integer idFuncionario;
    private String nombreFuncionario;

    @Schema(description = "Instancia generada al atenderla; null salvo en ATENDIDA")
    private Integer codInstancia;
    private String idNegInstancia;

    private LocalDateTime fechaResolucion;
    private LocalDateTime createdAt;
}
