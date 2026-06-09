// dto/LoginResponseDTO.java
package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Respuesta del login con el token JWT")
public class LoginResponseDTO {

    @Schema(description = "Token JWT para usar en los requests")
    private String token;
}