package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * DTO de salida resumido — requiere permiso VER_ESTUDIANTE_RESUMIDO.
 * Roles: todos (ADMINISTRADOR, PSICOPEDAGOGO, TUTOR, DOCENTE, LEGALES).
 *
 * Expone solo lo necesario para identificar y localizar un estudiante.
 * Sin datos de salud, sin motivo de derivación, sin foto.
 * Es la vista que usan roles de menor privilegio como TUTOR y DOCENTE.
 */
@Getter
@Builder
@Schema(description = "Datos resumidos de un estudiante")
public class EstudianteResumenDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Integer idUsuario;

    private String nombre;
    private String apellido;
    private String documento;
    private String email;
    private EstadoLogico estado;

    @Schema(description = "Grupos académicos actuales del estudiante")
    private List<String> grupos;

    @Schema(description = "Teléfonos de contacto")
    private List<String> telefonos;
}