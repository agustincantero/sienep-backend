package com.fides.sienep.dto;

import com.fides.sienep.model.PaisDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para crear o actualizar un Funcionario.
 *
 * Separa la entidad JPA de lo que llega por HTTP.
 *
 * Ventajas:
 * - Evita mass assignment.
 * - Centraliza las validaciones.
 * - Facilita la documentación OpenAPI.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para registrar o actualizar un funcionario")
public class FuncionarioRequestDTO {

    @NotBlank
    @Size(max = 30)
    @Schema(description = "Nombre del funcionario", example = "Juan")
    private String nombre;

    @NotBlank
    @Size(max = 30)
    @Schema(description = "Apellido del funcionario", example = "Pérez")
    private String apellido;

    @NotBlank
    @Size(max = 12)
    @Schema(description = "Número de documento", example = "12345678")
    private String documento;

    @NotNull
    @Schema(description = "País emisor del documento", example = "URY")
    private PaisDocumento paisDocumento;

    @NotBlank
    @Size(min = 8, max = 100)
    @Schema(
            description = "Contraseña inicial",
            example = "Temp1234!"
    )
    private String contrasenia;

    @NotBlank
    @Email
    @Schema(description = "Correo institucional", example = "juan.perez@utec.edu.uy")
    private String email;

    @NotNull
    @Schema(description = "ID del rol asignado al funcionario", example = "1")
    private Integer idRol;
}