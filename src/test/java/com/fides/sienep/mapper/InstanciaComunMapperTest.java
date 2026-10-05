package com.fides.sienep.mapper;

import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.InstanciaComun;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstanciaComunMapperTest {

    private final InstanciaComunMapper mapper = new InstanciaComunMapperImpl();

    private InstanciaComun instancia() {
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

        CategoriaInstancia categoria = new CategoriaInstancia();
        categoria.setIdCategoria(3);
        categoria.setNomCategoria("Seguimiento");

        InstanciaComun instancia = new InstanciaComun();
        instancia.setCodInstancia(1);
        instancia.setEstudiante(estudiante);
        instancia.setFuncionario(funcionario);
        instancia.setCategoria(categoria);
        return instancia;
    }

    @Test
    @DisplayName("toDto - Incluye el documento del estudiante y del funcionario junto a sus nombres")
    void toDto_IncluyeLosDocumentos() {
        InstanciaComunResponseDTO dto = mapper.toDto(instancia());

        assertEquals("Ana Pérez", dto.getNombreEstudiante());
        assertEquals("43867154", dto.getDocumentoEstudiante());
        assertEquals("Carlos Gómez", dto.getNombreFuncionario());
        assertEquals("11111111", dto.getDocumentoFuncionario());
    }

    @Test
    @DisplayName("toDtoList - Cada elemento lleva sus documentos")
    void toDtoList_IncluyeLosDocumentos() {
        var dtos = mapper.toDtoList(java.util.List.of(instancia()));

        assertEquals(1, dtos.size());
        assertEquals("43867154", dtos.get(0).getDocumentoEstudiante());
        assertEquals("11111111", dtos.get(0).getDocumentoFuncionario());
    }
}
