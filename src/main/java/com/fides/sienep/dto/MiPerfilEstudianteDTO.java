package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.PaisDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de salida del perfil propio del estudiante (GET /estudiantes/mi-perfil y la respuesta de
 * PUT /estudiantes/{id}/mis-datos).
 *
 * A diferencia de EstudianteResumenDTO (que también ven roles como TUTOR o DOCENTE), incluye la
 * dirección: sin ella el estudiante no puede editar sus datos de contacto, porque mis-datos es un
 * PUT de reemplazo completo y mandar solo los teléfonos le borraría la dirección.
 *
 * Deliberadamente NO incluye informacionSalud, sistemaSalud ni motivoDerivacion: son el bloque
 * confidencial y la evaluación institucional, que no se exponen por el autoservicio.
 */
@Getter
@Setter
@Builder
@Schema(description = "Perfil propio del estudiante autenticado")
public class MiPerfilEstudianteDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Integer idUsuario;

    private String nombre;
    private String apellido;
    private String documento;
    private PaisDocumento paisDocumento;
    private String email;
    private EstadoLogico estado;
    private LocalDate fechaNacimiento;

    @Schema(example = "Montevideo")
    private String ciudad;

    @Schema(example = "Montevideo")
    private String departamento;

    @Schema(example = "Av. 18 de Julio")
    private String calle;

    @Schema(example = "1234")
    private Integer nroPuerta;

    @Schema(description = "Grupos académicos actuales del estudiante")
    private List<String> grupos;

    @Schema(description = "Teléfonos de contacto")
    private List<String> telefonos;

    @Schema(description = "Ruta para ver la foto de perfil. Es null si el estudiante no tiene foto cargada.",
            example = "/estudiantes/5/foto")
    private String urlFoto;
}
