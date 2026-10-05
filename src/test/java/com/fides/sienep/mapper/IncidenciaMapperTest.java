package com.fides.sienep.mapper;

import com.fides.sienep.dto.IncidenciaResponseDTO;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Incidencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IncidenciaMapperTest {

    private final IncidenciaMapper mapper = new IncidenciaMapperImpl();

    private Incidencia incidencia() {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(5);
        estudiante.setNombre("Ana");
        estudiante.setApellido("Pérez");
        estudiante.setDocumento("43867154");

        Funcionario funcionario = new Funcionario();
        funcionario.setIdUsuario(2);
        funcionario.setNombre("Carlos");
        funcionario.setApellido("Gómez");
        funcionario.setDocumento("11111111");

        Incidencia incidencia = new Incidencia();
        incidencia.setCodInstancia(1);
        incidencia.setEstudiante(estudiante);
        incidencia.setFuncionario(funcionario);
        return incidencia;
    }

    @Test
    @DisplayName("toDto - Incluye el documento del estudiante y del funcionario junto a sus nombres")
    void toDto_IncluyeLosDocumentos() {
        IncidenciaResponseDTO dto = mapper.toDto(incidencia());

        assertEquals("Ana Pérez", dto.getNombreEstudiante());
        assertEquals("43867154", dto.getDocumentoEstudiante());
        assertEquals("Carlos Gómez", dto.getNombreFuncionario());
        assertEquals("11111111", dto.getDocumentoFuncionario());
    }

    @Test
    @DisplayName("toDtoList - Cada elemento lleva sus documentos")
    void toDtoList_IncluyeLosDocumentos() {
        var dtos = mapper.toDtoList(java.util.List.of(incidencia()));

        assertEquals(1, dtos.size());
        assertEquals("43867154", dtos.get(0).getDocumentoEstudiante());
        assertEquals("11111111", dtos.get(0).getDocumentoFuncionario());
    }
}
