package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para adjuntar un informe a un estudiante")
public class InformeAdjuntoRequestDTO {

    @NotNull(message = "El id del estudiante es obligatorio")
    @Schema(description = "ID del estudiante", example = "5")
    private Integer idUsuario;

    @NotBlank(message = "La URL del archivo es obligatoria")
    @Pattern(
            regexp = "^[A-Za-z0-9_./:%?&=-]+\\.(pdf|jpg|jpeg|png)$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "La URL debe terminar en .pdf, .jpg, .jpeg o .png y no contener caracteres inválidos"
    )
    @Schema(description = "URL de acceso al archivo",
            example = "https://storage.example.com/informes/informe_2024.pdf")
    private String urlArchivo;

    @Pattern(
            regexp = "^[A-Za-z0-9_./:-]{1,255}$",
            message = "La ruta contiene caracteres no permitidos"
    )
    @Schema(description = "Ruta del archivo (opcional)", example = "/informes/2024/")
    private String ruta;

    @NotBlank(message = "El nombre del archivo es obligatorio")
    @Pattern(
            regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 _-]{1,50}$",
            message = "El nombre contiene caracteres no permitidos"
    )
    @Schema(description = "Nombre del archivo", example = "informe_psicopedagogico_2024")
    private String nombre;
}