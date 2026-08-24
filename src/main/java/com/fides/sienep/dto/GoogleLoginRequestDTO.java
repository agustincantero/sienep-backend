// dto/GoogleLoginRequestDTO.java
package com.fides.sienep.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "ID token de Google obtenido en el frontend vía Google Identity Services")
public class GoogleLoginRequestDTO {

    @NotBlank
    @Schema(description = "ID token (JWT) que Google emite al usuario tras autenticarse")
    private String idToken;
}
