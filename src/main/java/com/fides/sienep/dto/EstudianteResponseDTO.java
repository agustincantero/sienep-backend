package com.fides.sienep.dto;

import com.fides.sienep.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
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
    private String foto;

    @Schema(description = "Números de teléfono del estudiante")
    private List<String> telefonos;

    @Schema(description = "Nombres de los grupos académicos del estudiante")
    private List<String> grupos;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
}