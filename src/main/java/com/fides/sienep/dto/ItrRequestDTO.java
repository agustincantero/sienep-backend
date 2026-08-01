package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para crear o editar un ITR")
public class ItrRequestDTO {

    @NotBlank(message = "El nombre del ITR es obligatorio.")
    @Size(max = 80, message = "El nombre del ITR no puede superar los 80 caracteres.")
    @Schema(description = "Nombre del ITR", example = "ITR Suroeste")
    private String nomItr;

    @NotNull(message = "La lista de carreras es obligatoria (puede estar vacía).")
    @Schema(description = "Lista de IDs de carreras a asociar al ITR", example = "[1, 2, 3]")
    private List<Integer> carrerasIds = new ArrayList<>();
}