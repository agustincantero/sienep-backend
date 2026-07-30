package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para adjuntar un informe a un estudiante (multipart/form-data)")
public class InformeAdjuntoRequestDTO {

    @NotNull(message = "El id del estudiante es obligatorio")
    @Schema(description = "ID del estudiante", example = "5")
    private Integer idUsuario;

    @NotBlank(message = "El nombre del archivo es obligatorio")
    @Pattern(
            regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 _-]{1,50}$",
            message = "El nombre contiene caracteres no permitidos"
    )
    @Schema(description = "Nombre del archivo", example = "informe_psicopedagogico_2024")
    private String nombre;

    @NotNull(message = "El archivo es obligatorio")
    @Schema(description = "Archivo a adjuntar (PDF, JPG, JPEG o PNG, máx. 5MB)", type = "string", format = "binary")
    private MultipartFile archivo;
}