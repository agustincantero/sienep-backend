package com.fides.sienep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RolCreateDTO {

    @NotBlank(message = "El nombre del rol no puede estar vacío")
    @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
    private String nombre;

    @Size(max = 100, message = "La descripción no puede superar los 100 caracteres")
    private String descripcion;

    // No se incluye el campo 'estado' porque el backend siempre fuerza ACTIVO en la creación.
    private List<Integer> permisosIds;
}
