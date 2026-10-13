package com.fides.sienep.mapper;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.InformeAdjunto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InformeAdjuntoMapperTest {

    private final InformeAdjuntoMapper mapper = new InformeAdjuntoMapperImpl();

    private InformeAdjunto informe() {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(5);
        return InformeAdjunto.builder().idInforme(3).estudiante(estudiante).nombre("informe").build();
    }

    @Test
    @DisplayName("Funcionarios - La descarga apunta al endpoint que exige VER_INFORME")
    void toDto_UrlDeFuncionarios() {
        assertEquals("/informes-adjuntos/3/descargar", mapper.toDto(informe()).getUrlDescarga());
    }

    @Test
    @DisplayName("Funcionarios - Un informe eliminado no lleva URL de descarga")
    void toDto_EliminadoSinUrl() {
        InformeAdjunto eliminado = informe();
        eliminado.setEstado(EstadoLogico.ELIMINADO);

        assertNull(mapper.toDto(eliminado).getUrlDescarga());
        assertEquals("informe", mapper.toDto(eliminado).getNombre());
        assertEquals(EstadoLogico.ELIMINADO, mapper.toDto(eliminado).getEstado());
    }

    @Test
    @DisplayName("Estudiante - La descarga apunta a su endpoint propio")
    void toDtoDelEstudiante_UrlPropia() {
        assertEquals("/informes-adjuntos/mios/3/descargar", mapper.toDtoDelEstudiante(informe()).getUrlDescarga());
        assertEquals(5, mapper.toDtoDelEstudiante(informe()).getIdUsuario());
    }
}
