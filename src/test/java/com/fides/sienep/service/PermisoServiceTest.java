package com.fides.sienep.service;

import com.fides.sienep.dto.PermisoResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.repository.PermisoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import com.fides.sienep.mapper.PermisoMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PermisoServiceTest {

    @Mock
    private PermisoRepository permisoRepository;

    @SuppressWarnings("unused")
    @Spy
    private PermisoMapper mapper = Mappers.getMapper(PermisoMapper.class);

    @InjectMocks
    private PermisoService permisoService;

    @Test
    @DisplayName("Listar permisos - Éxito")
    void listar_DebeRetornarListaDePermisos() {
        // Arrange
        Permiso p1 = new Permiso("PERMISO_1", 1, "desc 1", EstadoLogico.ACTIVO);
        Permiso p2 = new Permiso("PERMISO_2", 2, "desc 2", EstadoLogico.ACTIVO);
        when(permisoRepository.findAll()).thenReturn(List.of(p1, p2));

        // Act
        List<PermisoResponseDTO> resultado = permisoService.listar();

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("PERMISO_1", resultado.get(0).getNombre());
        verify(permisoRepository, times(1)).findAll();
    }
}
