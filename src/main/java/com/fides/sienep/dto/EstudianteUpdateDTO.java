package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO exclusivo para PUT /api/estudiantes/{id}
 *
 * ¿Por qué existe este DTO separado del EstudianteRequestDTO?
 *
 * Documento y paisDocumento son INMUTABLES después del alta.
 * Son la identidad del estudiante en el sistema — cambiarlos
 * implicaría un proceso institucional formal, no un PUT simple.
 *
 * Al no incluirlos en este DTO:
 *   1. Es IMPOSIBLE que lleguen por este endpoint (Spring ignora
 *      campos JSON que no tienen campo correspondiente en el DTO).
 *   2. No necesitamos validar ni lanzar error — el problema
 *      desaparece en la capa de entrada.
 *   3. Swagger documenta claramente qué se puede editar y qué no.
 *
 * Este patrón se llama "DTOs especializados por operación" y es
 * una buena práctica de diseño de APIs REST.
 *
 * También se excluye contrasenia: el cambio de contraseña tiene
 * su propio endpoint PUT /{id}/contrasenia con flujo dedicado.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos modificables de un estudiante. Documento, paisDocumento y contraseña NO son editables por este endpoint.")
public class EstudianteUpdateDTO {

    @NotBlank
    @Size(max = 30)
    @Schema(description = "Nombre del estudiante", example = "María")
    private String nombre;

    @NotBlank
    @Size(max = 30)
    @Schema(description = "Apellido del estudiante", example = "González")
    private String apellido;

    @NotBlank
    @Email
    @Schema(description = "Email institucional", example = "maria.gonzalez@utec.edu.uy")
    private String email;

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

    @Schema(description = "Información de salud general")
    private String informacionSalud;

    @Size(max = 80)
    @Schema(example = "ASSE")
    private String sistemaSalud;

    @NotNull
    @Past
    @Schema(description = "Fecha de nacimiento", example = "2000-05-15")
    private LocalDate fechaNacimiento;

    @Schema(description = "Motivo de derivación al servicio psicopedagógico")
    private String motivoDerivacion;

    @Size(max = 255)
    @Schema(description = "URL o ruta de la foto")
    private String foto;

    @Schema(description = "Lista de números de teléfono", example = "[\"099123456\", \"22001122\"]")
    private List<@NotBlank @Size(min = 8, max = 15) String> telefonos;

    @Schema(description = "IDs de los grupos a asignar")
    private List<Integer> idGrupos;
}