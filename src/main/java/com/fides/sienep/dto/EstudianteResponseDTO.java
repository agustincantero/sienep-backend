package com.fides.sienep.dto;

import com.fides.sienep.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de salida completo — requiere permiso VER_ESTUDIANTE.
 * Roles: ADMINISTRADOR, PSICOPEDAGOGO, LEGALES.
 *
 * Incluye todos los campos del estudiante excepto contraseña
 * y comentarios confidenciales (tienen su propio endpoint y permiso).
 *
 * @Builder: permite construir el objeto en el Service con un patrón
 * fluido sin necesitar un constructor largo. El Controller nunca
 * construye este DTO — solo lo recibe del Service y lo devuelve.
 */
@Getter
@Builder
@Schema(description = "Datos completos de un estudiante — acceso restringido")
public class EstudianteResponseDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Integer idUsuario;

    private String nombre;
    private String apellido;
    private String documento;
    private PaisDocumento paisDocumento;
    private String email;
    private EstadoLogico estado;
    private LocalDate fechaNacimiento;

    private String ciudad;
    private String departamento;
    private String calle;
    private Integer nroPuerta;

    @Schema(description = "Información de salud — visible solo con VER_ESTUDIANTE")
    private String informacionSalud;
    private String sistemaSalud;
    private String motivoDerivacion;
    private String foto;

    private List<String> telefonos;
    private List<String> grupos;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}