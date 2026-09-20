package com.fides.sienep.mapper;

import com.fides.sienep.dto.EstudianteResponseDTO;
import com.fides.sienep.dto.EstudianteResumenDTO;
import com.fides.sienep.model.Estudiante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EstudianteMapperTest {

    private final EstudianteMapper mapper = new EstudianteMapperImpl();

    private Estudiante estudiante(String foto) {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(7);
        estudiante.setNombre("Ana");
        estudiante.setApellido("García");
        estudiante.setFoto(foto);
        estudiante.setTelefonos(new ArrayList<>());
        estudiante.setGrupos(new ArrayList<>());
        return estudiante;
    }

    @Test
    @DisplayName("Resumen - Con foto cargada arma la urlFoto con el id del estudiante")
    void toResumenDto_ConFoto_ArmaLaUrl() {
        EstudianteResumenDTO dto = mapper.toResumenDto(estudiante("foto_7.png"));

        assertEquals("/estudiantes/7/foto", dto.getUrlFoto());
    }

    @Test
    @DisplayName("Resumen - Sin foto cargada la urlFoto es null")
    void toResumenDto_SinFoto_UrlNull() {
        EstudianteResumenDTO dto = mapper.toResumenDto(estudiante(null));

        assertNull(dto.getUrlFoto());
    }

    @Test
    @DisplayName("Ficha completa - Con foto cargada sigue armando la misma urlFoto")
    void toDto_ConFoto_ArmaLaUrl() {
        EstudianteResponseDTO dto = mapper.toDto(estudiante("foto_7.png"));

        assertEquals("/estudiantes/7/foto", dto.getUrlFoto());
    }
}
