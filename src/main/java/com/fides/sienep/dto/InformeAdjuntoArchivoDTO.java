package com.fides.sienep.dto;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record InformeAdjuntoArchivoDTO(Resource recurso, MediaType contentType, String nombreDescarga) {
}
