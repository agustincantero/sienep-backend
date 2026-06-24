package com.fides.sienep.dto;

import com.fides.sienep.model.PaisDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
// me parece que corresponde NoArgsConstructor
@NoArgsConstructor
//
@Schema(description = "Datos para registrar un funcionario")
public class FuncionarioRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede tener más de 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]+$", message = "Solo se permiten letras, números y espacios")
    @Schema(description = "Nombre del funcionario", example = "Carlos")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 50, message = "El apellido no puede tener más de 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]+$", message = "Solo se permiten letras, números y espacios")
    @Schema(description = "Apellido del funcionario", example = "Pérez")
    private String apellido;

    @NotBlank(message = "El documento es obligatorio")
    @Size(max = 15)
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "El documento debe ser alfanumérico")
    @Schema(description = "Documento de identidad", example = "12345678")
    private String documento;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    @Schema(description = "Correo electrónico institucional", example = "carlos.perez@utec.edu.uy")
    private String email;

    @NotNull(message = "El ID de rol es obligatorio")
    @Schema(description = "ID del rol asignado", example = "1")
    private Integer idRol;


    // ========================================================
    // SOLE
    @NotNull
    @Schema(description = "País emisor del documento", example = "URY")
    private PaisDocumento paisDocumento;


    // ========================================================
}