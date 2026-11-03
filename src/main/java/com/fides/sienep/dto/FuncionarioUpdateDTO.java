package com.fides.sienep.dto;

import com.fides.sienep.utils.ReglasDeTexto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para editar un funcionario")
public class FuncionarioUpdateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede tener más de 50 caracteres")
    @Pattern(regexp = ReglasDeTexto.NOMBRE_PERSONA_REGEX, message = ReglasDeTexto.NOMBRE_PERSONA_MSG)
    @Schema(description = "Nombre del funcionario", example = "Carlos")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 50, message = "El apellido no puede tener más de 50 caracteres")
    @Pattern(regexp = ReglasDeTexto.NOMBRE_PERSONA_REGEX, message = ReglasDeTexto.NOMBRE_PERSONA_MSG)
    @Schema(description = "Apellido del funcionario", example = "Pérez")
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Debe ser un email válido")
    @Schema(description = "Correo electrónico institucional", example = "carlos.perez@utec.edu.uy")
    private String email;

    @NotNull(message = "El ID de rol es obligatorio")
    @Schema(description = "ID del rol asignado", example = "1")
    private Integer idRol;
}