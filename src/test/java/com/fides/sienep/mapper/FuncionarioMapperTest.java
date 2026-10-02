package com.fides.sienep.mapper;

import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FuncionarioMapperTest {

    private final FuncionarioMapper mapper = new FuncionarioMapperImpl();

    private Funcionario funcionario(String foto) {
        Funcionario funcionario = new Funcionario();
        funcionario.setIdUsuario(3);
        funcionario.setNombre("Carlos");
        funcionario.setRol(new Rol("ADMINISTRADOR"));
        funcionario.setFoto(foto);
        return funcionario;
    }

    @Test
    @DisplayName("Con foto cargada arma la urlFoto con el id del funcionario")
    void toDto_ConFoto_ArmaLaUrl() {
        FuncionarioResponseDTO dto = mapper.toDto(funcionario("fotos/3/abc.png"));

        assertEquals("/funcionarios/3/foto", dto.getUrlFoto());
        assertEquals("ADMINISTRADOR", dto.getRol());
    }

    @Test
    @DisplayName("Sin foto cargada la urlFoto es null")
    void toDto_SinFoto_UrlNull() {
        assertNull(mapper.toDto(funcionario(null)).getUrlFoto());
    }
}
