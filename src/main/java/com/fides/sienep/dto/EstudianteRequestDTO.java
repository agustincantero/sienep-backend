package com.fides.sienep.dto;

import com.fides.sienep.utils.ReglasDeTexto;
import com.fides.sienep.model.PaisDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de entrada para crear o editar un Estudiante.
 *
 * Separa la entidad JPA de lo que llega por HTTP.
 * Ventajas:
 *  - Evita mass assignment: el cliente no puede setear estado, idUsuario, timestamps.
 *  - Las validaciones (@NotBlank, @Email, etc.) viven acá, no en la entidad.
 *  - La entidad solo cambia si el Service lo decide explícitamente.
 *
 * contrasenia solo se usa en el alta. En actualizaciones el Service la ignora
 * si viene vacía — el cambio de contraseña tiene su propio endpoint.
 *
 * No incluye foto: la foto de perfil se sube como archivo real (multipart)
 * vía PUT /api/estudiantes/{id}/foto, requiere que el estudiante ya exista.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para registrar o editar un estudiante")
public class EstudianteRequestDTO {

    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = ReglasDeTexto.NOMBRE_PERSONA_REGEX, message = ReglasDeTexto.NOMBRE_PERSONA_MSG)
    @Schema(description = "Nombre del estudiante", example = "María")
    private String nombre;

    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = ReglasDeTexto.NOMBRE_PERSONA_REGEX, message = ReglasDeTexto.NOMBRE_PERSONA_MSG)
    @Schema(description = "Apellido del estudiante", example = "González")
    private String apellido;

    @NotBlank
    @Size(max = 12)
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "El documento debe ser alfanumérico sin espacios ni caracteres especiales")
    @Schema(description = "Número de documento (CI)", example = "12345678")
    private String documento;

    @NotNull
    @Schema(description = "País emisor del documento", example = "URY")
    private PaisDocumento paisDocumento;

    @NotBlank
    @Email
    @Schema(description = "Email institucional", example = "maria.gonzalez@utec.edu.uy")
    private String email;
    

    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]+$", message = "Solo se permiten letras, números y espacios")
    @Schema(example = "Montevideo")
    private String ciudad;

    @Size(max = 20)
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]+$", message = "Solo se permiten letras, números y espacios")
    @Schema(example = "Montevideo")
    private String departamento;

    @Size(max = 160)
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]+$", message = "Solo se permiten letras, números y espacios")
    @Schema(example = "Av 18 de Julio")
    private String calle;

    @Min(1) @Max(9999)
    @Schema(example = "1234")
    private Integer nroPuerta;

    @Schema(description = "Información de salud general")
    private String informacionSalud;

    @Size(max = 80)
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]+$", message = "Solo se permiten letras, números y espacios")
    @Schema(example = "ASSE")
    private String sistemaSalud;

    @NotNull
    @Past
    @Schema(description = "Fecha de nacimiento — debe ser mayor de 18 años", example = "2000-05-15")
    private LocalDate fechaNacimiento;

    @Schema(description = "Motivo de derivación al servicio psicopedagógico")
    private String motivoDerivacion;

    @Schema(description = "Lista de números de teléfono", example = "[\"099123456\", \"22001122\"]")
    private List<@NotBlank @Pattern(regexp = "^[0-9]{8,12}$", message = "El teléfono debe contener entre 8 y 12 dígitos numéricos") String> telefonos;

    @Schema(description = "IDs de los grupos a asignar")
    private List<Integer> idGrupos;
}