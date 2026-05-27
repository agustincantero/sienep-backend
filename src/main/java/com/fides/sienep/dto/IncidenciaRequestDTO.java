package com.fides.sienep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class IncidenciaRequestDTO {

    @NotBlank(message = "El título no puede estar vacío")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    private String titulo;

    @NotNull(message = "La fecha y hora no puede ser nula")
    private LocalDateTime fechaHora;

    @NotNull(message = "El estudiante no puede ser nulo")
    private Integer idEstudiante;

    @NotNull(message = "El funcionario no puede ser nulo")
    private Integer idFuncionario;

    @Size(max = 50, message = "El canal no puede superar los 50 caracteres")
    private String canal;

    @NotBlank(message = "El lugar no puede estar vacío")
    @Size(max = 50, message = "El lugar no puede superar los 50 caracteres")
    private String lugar;
}