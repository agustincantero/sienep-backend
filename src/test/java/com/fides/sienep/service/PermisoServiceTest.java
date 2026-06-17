package com.fides.sienep.service;

import com.fides.sienep.dto.PermisoResponseDTO;
import com.fides.sienep.dto.PermisoUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.repository.PermisoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import com.fides.sienep.mapper.PermisoMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    void obtenerTodos_DebeRetornarListaDePermisos() {
        // Arrange
        Permiso p1 = new Permiso("PERMISO_1", 1, "desc 1", EstadoLogico.ACTIVO);
        Permiso p2 = new Permiso("PERMISO_2", 2, "desc 2", EstadoLogico.ACTIVO);
        when(permisoRepository.findAll()).thenReturn(List.of(p1, p2));

        // Act
        List<PermisoResponseDTO> resultado = permisoService.obtenerTodos();

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("PERMISO_1", resultado.get(0).getNombre());
        verify(permisoRepository, times(1)).findAll();
    }

    @Test
    void actualizarDescripcion_Exito() {
        // Arrange
        int permisoId = 1;
        Permiso permisoExistente = new Permiso("EDITAR_ALGO", permisoId, "Vieja descripcion", EstadoLogico.ACTIVO);
        
        PermisoUpdateDTO updateDTO = new PermisoUpdateDTO();
        updateDTO.setDescripcion("Nueva descripcion detallada");

        when(permisoRepository.findById(permisoId)).thenReturn(Optional.of(permisoExistente));
        when(permisoRepository.save(any(Permiso.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PermisoResponseDTO resultado = permisoService.actualizarDescripcion(permisoId, updateDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals("Nueva descripcion detallada", resultado.getDescripcion());
        verify(permisoRepository, times(1)).findById(permisoId);
        verify(permisoRepository, times(1)).save(permisoExistente);
    }

    @Test
    void actualizarDescripcion_LanzaNotFound_CuandoPermisoNoExiste() {
        // Arrange
        int permisoId = 99;
        PermisoUpdateDTO updateDTO = new PermisoUpdateDTO();
        updateDTO.setDescripcion("Nueva desc");

        when(permisoRepository.findById(permisoId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            permisoService.actualizarDescripcion(permisoId, updateDTO);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Permiso no encontrado", exception.getReason());
        verify(permisoRepository, never()).save(any(Permiso.class));
    }
}
