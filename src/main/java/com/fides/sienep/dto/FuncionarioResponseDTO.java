package com.fides.sienep.dto;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.PaisDocumento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos completos de un funcionario")
public class FuncionarioResponseDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        private Integer idUsuario;

        private String nombre;

        private String apellido;

        private String documento;

        private PaisDocumento paisDocumento;

        private String email;

        private EstadoLogico estado;

        @Schema(description = "Nombre del rol asignado")
        private String rol;

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        private LocalDateTime createdAt;

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        private LocalDateTime updatedAt;

        @Schema(description = "Advertencia sobre el alta (ej. no se pudo enviar el email con la contraseña temporal). " +
                "Solo se completa en la respuesta de creación, null en el resto de las consultas.",
                accessMode = Schema.AccessMode.READ_ONLY)
        private String advertencia;
    }