package com.fides.sienep.service;

import com.fides.sienep.dto.CategoriaInstanciaCreateDTO;
import com.fides.sienep.dto.CategoriaInstanciaResponseDTO;
import com.fides.sienep.dto.CategoriaInstanciaUpdateDTO;
import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.CategoriaInstanciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import com.fides.sienep.mapper.CategoriaInstanciaMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoriaInstanciaServiceTest {

    @Mock
    private CategoriaInstanciaRepository categoriaRepository;

    @SuppressWarnings("unused")
    @Spy
    private CategoriaInstanciaMapper mapper = Mappers.getMapper(CategoriaInstanciaMapper.class);

    @InjectMocks
    private CategoriaInstanciaService categoriaService;

    @Test
    void obtenerTodas_DebeRetornarListaDeCategorias() {
        CategoriaInstancia cat = new CategoriaInstancia();
        cat.setIdCategoria(1);
        cat.setNomCategoria("CAT_1");
        cat.setEstado(EstadoLogico.ACTIVO);
        
        when(categoriaRepository.buscarConFiltros("CAT", EstadoLogico.ACTIVO)).thenReturn(List.of(cat));

        List<CategoriaInstanciaResponseDTO> resultado = categoriaService.obtenerTodas("CAT", EstadoLogico.ACTIVO);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("CAT_1", resultado.get(0).getNomCategoria());
    }

    @Test
    void obtenerPorId_Exito() {
        CategoriaInstancia cat = new CategoriaInstancia();
        cat.setIdCategoria(1);
        cat.setNomCategoria("CAT_1");
        
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(cat));

        CategoriaInstanciaResponseDTO resultado = categoriaService.obtenerPorId(1);

        assertNotNull(resultado);
        assertEquals("CAT_1", resultado.getNomCategoria());
    }

    @Test
    void obtenerPorId_LanzaNotFound() {
        when(categoriaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> categoriaService.obtenerPorId(99));
    }

    @Test
    void crearCategoria_Exito() {
        CategoriaInstanciaCreateDTO dto = new CategoriaInstanciaCreateDTO();
        dto.setNomCategoria("  Categoria Nueva  ");
        dto.setDescripcion("Una descripcion");

        when(categoriaRepository.existsByNomCategoria("CATEGORIA_NUEVA")).thenReturn(false);
        when(categoriaRepository.save(any(CategoriaInstancia.class))).thenAnswer(invocation -> {
            CategoriaInstancia guardada = invocation.getArgument(0);
            guardada.setIdCategoria(1);
            return guardada;
        });

        CategoriaInstanciaResponseDTO resultado = categoriaService.crearCategoria(dto);

        assertNotNull(resultado);
        assertEquals(1, resultado.getIdCategoria());
        assertEquals("CATEGORIA_NUEVA", resultado.getNomCategoria());
        assertEquals(EstadoLogico.ACTIVO, resultado.getEstado());
    }

    @Test
    void crearCategoria_LanzaConflict_CuandoNombreYaExiste() {
        CategoriaInstanciaCreateDTO dto = new CategoriaInstanciaCreateDTO();
        dto.setNomCategoria("Existente");

        when(categoriaRepository.existsByNomCategoria("EXISTENTE")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> categoriaService.crearCategoria(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void actualizarCategoria_Exito() {
        CategoriaInstancia cat = new CategoriaInstancia();
        cat.setIdCategoria(1);
        cat.setNomCategoria("VIEJA");
        
        CategoriaInstanciaUpdateDTO dto = new CategoriaInstanciaUpdateDTO();
        dto.setNomCategoria("NUEVA");
        dto.setEstado(EstadoLogico.ACTIVO);

        when(categoriaRepository.findById(1)).thenReturn(Optional.of(cat));
        when(categoriaRepository.existsByNomCategoria("NUEVA")).thenReturn(false);
        when(categoriaRepository.save(any(CategoriaInstancia.class))).thenAnswer(i -> i.getArgument(0));

        CategoriaInstanciaResponseDTO resultado = categoriaService.actualizarCategoria(1, dto);

        assertNotNull(resultado);
        assertEquals("NUEVA", resultado.getNomCategoria());
    }

    @Test
    void actualizarCategoria_LanzaConflict_CuandoNombreYaExiste() {
        CategoriaInstancia cat = new CategoriaInstancia();
        cat.setIdCategoria(1);
        cat.setNomCategoria("VIEJA");
        
        CategoriaInstanciaUpdateDTO dto = new CategoriaInstanciaUpdateDTO();
        dto.setNomCategoria("OTRA_EXISTENTE");

        when(categoriaRepository.findById(1)).thenReturn(Optional.of(cat));
        when(categoriaRepository.existsByNomCategoria("OTRA_EXISTENTE")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> categoriaService.actualizarCategoria(1, dto));
    }

    @Test
    void darDeBajaCategoria_Exito() {
        CategoriaInstancia cat = new CategoriaInstancia();
        cat.setIdCategoria(5);
        cat.setEstado(EstadoLogico.ACTIVO);

        when(categoriaRepository.findById(5)).thenReturn(Optional.of(cat));

        categoriaService.darDeBajaCategoria(5);

        assertEquals(EstadoLogico.INACTIVO, cat.getEstado());
        verify(categoriaRepository).save(cat);
    }
    
    @Test
    void darDeBajaCategoria_LanzaNotFound() {
        when(categoriaRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> categoriaService.darDeBajaCategoria(99));
    }
}
