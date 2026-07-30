package com.fides.sienep.dto;

import com.fides.sienep.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@Schema(description = "Datos completos de un estudiante — requiere VER_ESTUDIANTE")
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
    private String informacionSalud;
    private String sistemaSalud;
    private String motivoDerivacion;

    @Schema(description = "Ruta para ver la foto de perfil (null si no tiene foto cargada). " +
            "Requiere autenticación y permiso VER_ESTUDIANTE, o ser el propio estudiante.",
            example = "/api/estudiantes/5/foto")
    private String urlFoto;

    @Schema(description = "Números de teléfono del estudiante")
    private List<String> telefonos;

    @Schema(description = "Nombres de los grupos académicos del estudiante")
    private List<String> grupos;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    @Schema(description = "Advertencia sobre el alta (ej. no se pudo enviar el email con la contraseña temporal). " +
            "Solo se completa en la respuesta de creación, null en el resto de las consultas.",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String advertencia;
}