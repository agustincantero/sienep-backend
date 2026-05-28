package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO de autoservicio del estudiante.
 *
 * Contiene SOLO los campos que un estudiante puede editar de sí mismo:
 * datos de contacto. Deliberadamente NO incluye:
 *   - nombre, apellido, documento, email, fechaNacimiento (datos de identidad)
 *   - informacionSalud, sistemaSalud (datos sensibles — cambio vía personal con comprobante)
 *   - motivoDerivacion (evaluación institucional)
 *   - grupos (asignación administrativa)
 *
 * Al ser un DTO separado de EstudianteRequestDTO, es imposible que un
 * estudiante envíe campos fuera de su alcance: aunque los incluya en el JSON,
 * este DTO no los tiene y Spring los ignora. Cierra el mass assignment por diseño.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos de contacto que un estudiante puede editar de sí mismo")
public class MisDatosRequestDTO {

    @Size(max = 50)
    @Schema(example = "Montevideo")
    private String ciudad;

    @Size(max = 20)
    @Schema(example = "Montevideo")
    private String departamento;

    @Size(max = 160)
    @Schema(example = "Av. 18 de Julio")
    private String calle;

    @Min(1) @Max(9999)
    @Schema(example = "1234")
    private Integer nroPuerta;

    @Schema(description = "Números de teléfono de contacto")
    private List<@NotBlank @Size(min = 8, max = 15) String> telefonos;
}