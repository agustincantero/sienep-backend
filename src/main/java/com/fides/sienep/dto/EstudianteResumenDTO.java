package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO de salida resumido — requiere permiso VER_ESTUDIANTE_RESUMIDO.
 * Roles: todos (ADMINISTRADOR, PSICOPEDAGOGO, TUTOR, DOCENTE, LEGALES).
 *
 * Expone solo lo necesario para identificar y localizar un estudiante.
 * Sin datos de salud ni motivo de derivación.
 * Es la vista que usan roles de menor privilegio como TUTOR y DOCENTE.
 *
 * La foto no viaja acá: solo se ofrece su ruta (urlFoto), y únicamente a quien puede pedirla
 * (VER_ESTUDIANTE, o el propio estudiante). Para el resto, urlFoto es null — ver
 * EstudianteService.
 */
@Getter
@Setter
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

    @Schema(description = "Ruta para ver la foto de perfil. Es null si el estudiante no tiene foto cargada " +
            "o si quien consulta no puede verla (requiere VER_ESTUDIANTE, o ser el propio estudiante).",
            example = "/estudiantes/5/foto")
    private String urlFoto;
}