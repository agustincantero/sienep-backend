package com.fides.sienep.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

@Getter
@AllArgsConstructor
public class ArchivoDescargaDTO {

    private final Resource recurso;
    private final MediaType contentType;
    private final String nombreDescarga;
}
