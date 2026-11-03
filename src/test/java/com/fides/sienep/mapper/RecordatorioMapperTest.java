package com.fides.sienep.mapper;

import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.model.CategoriaRecordatorio;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.model.Recordatorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordatorioMapperTest {

    private final RecordatorioMapper mapper = new RecordatorioMapperImpl();

    @Test
    @DisplayName("toDto - Incluye el identificador de negocio de la instancia")
    void toDto_IncluyeIdNegInstancia() {
        InstanciaComun instancia = new InstanciaComun();
        instancia.setCodInstancia(204);
        instancia.setIdNegInstancia("INST-2026-000204");
        instancia.setTitulo("Reunión de seguimiento");

        Funcionario responsable = new Funcionario();
        responsable.setNombre("Carlos");
        responsable.setApellido("Gómez");

        Recordatorio recordatorio = new Recordatorio();
        recordatorio.setInstancia(instancia);
        recordatorio.setCategoria(new CategoriaRecordatorio());
        recordatorio.setResponsable(responsable);

        RecordatorioResponseDTO dto = mapper.toDto(recordatorio);

        assertEquals(204, dto.getCodInstancia());
        assertEquals("INST-2026-000204", dto.getIdNegInstancia());
        assertEquals("Reunión de seguimiento", dto.getTituloInstancia());
    }
}
