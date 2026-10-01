package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.TipoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Respuesta de GET /auth/me: identidad y capacidades del usuario del token.
 *
 * Pensado para que el frontend, tras el login, gatee la navegación y la UI:
 *   - {@code tipo} define el shell (funcionario vs estudiante);
 *   - {@code permisos} define qué acciones mostrar/ocultar.
 *
 * Siempre acotado al usuario autenticado — el endpoint no recibe ningún id.
 */
@Getter
@Builder
@Schema(description = "Identidad y permisos del usuario autenticado")
public class UsuarioAutenticadoDTO {

    private Integer idUsuario;

    @Schema(description = "Subtipo de usuario: FUNCIONARIO o ESTUDIANTE")
    private TipoUsuario tipo;

    private String email;
    private String nombre;
    private String apellido;
    private EstadoLogico estado;

    /**
     * Nombre del rol ({@code nom_rol}, ej. "ADMINISTRADOR").
     * Para mostrar y diagnóstico; NO usar para autorización — el gating va por {@link #permisos}.
     */
    @Schema(description = "Nombre del rol. Solo display/diagnóstico; para gatear usar 'permisos'.")
    private String rol;

    @Schema(description = "Nombres de permiso (nom_permiso) activos del rol, sin duplicados y ordenados")
    private List<String> permisos;

    @Schema(description = "Ruta para ver la foto de perfil propia (/estudiantes/{id}/foto o /funcionarios/{id}/foto). " +
            "Es null si no tiene foto cargada.", example = "/funcionarios/3/foto")
    private String urlFoto;
}
