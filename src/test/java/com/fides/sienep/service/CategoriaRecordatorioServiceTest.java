package com.fides.sienep.service;

import com.fides.sienep.dto.CategoriaRecordatorioCreateDTO;
import com.fides.sienep.dto.CategoriaRecordatorioResponseDTO;
import com.fides.sienep.dto.CategoriaRecordatorioUpdateDTO;
import com.fides.sienep.model.CategoriaRecordatorio;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.CategoriaRecordatorioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import com.fides.sienep.mapper.CategoriaRecordatorioMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoriaRecordatorioServiceTest {

    @Mock
    private CategoriaRecordatorioRepository categoriaRepository;

    @SuppressWarnings("unused")
    @Spy
    private CategoriaRecordatorioMapper mapper = Mappers.getMapper(CategoriaRecordatorioMapper.class);

    @InjectMocks
    private CategoriaRecordatorioService categoriaService;

    @Test
    @DisplayName("Listar categorías de recordatorio - Éxito")
    void obtenerTodas_DebeRetornarListaDeCategorias() {
        CategoriaRecordatorio cat = new CategoriaRecordatorio();
        cat.setIdCategoria(1);
        cat.setNomCategoria("CAT_REC");
        cat.setEstado(EstadoLogico.ACTIVO);
        
        when(categoriaRepository.buscarConFiltros("CAT", EstadoLogico.ACTIVO)).thenReturn(List.of(cat));

        List<CategoriaRecordatorioResponseDTO> resultado = categoriaService.obtenerTodas("CAT", EstadoLogico.ACTIVO);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("CAT_REC", resultado.get(0).getNomCategoria());
    }

    @Test
    @DisplayName("Obtener categoría por ID - Éxito")
    void obtenerPorId_Exito() {
        CategoriaRecordatorio cat = new CategoriaRecordatorio();
        cat.setIdCategoria(1);
        cat.setNomCategoria("CAT_REC");
        
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(cat));

        CategoriaRecordatorioResponseDTO resultado = categoriaService.obtenerPorId(1);

        assertNotNull(resultado);
        assertEquals("CAT_REC", resultado.getNomCategoria());
    }

    @Test
    @DisplayName("Obtener categoría por ID - No encontrada (404)")
    void obtenerPorId_LanzaNotFound() {
        when(categoriaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> categoriaService.obtenerPorId(99));
    }

    @Test
    @DisplayName("Crear categoría - Éxito")
    void crearCategoria_Exito() {
        CategoriaRecordatorioCreateDTO dto = new CategoriaRecordatorioCreateDTO();
        dto.setNomCategoria("  Recordatorio Nuevo  ");
        dto.setDescripcion("Una descripcion");

        when(categoriaRepository.existsByNomCategoria("RECORDATORIO_NUEVO")).thenReturn(false);
        when(categoriaRepository.save(any(CategoriaRecordatorio.class))).thenAnswer(invocation -> {
            CategoriaRecordatorio guardada = invocation.getArgument(0);
            guardada.setIdCategoria(1);
            return guardada;
        });

        CategoriaRecordatorioResponseDTO resultado = categoriaService.crearCategoria(dto);

        assertNotNull(resultado);
        assertEquals(1, resultado.getIdCategoria());
        assertEquals("RECORDATORIO_NUEVO", resultado.getNomCategoria());
        assertEquals(EstadoLogico.ACTIVO, resultado.getEstado());
    }

    @Test
    @DisplayName("Crear categoría - Conflicto si nombre ya existe (409)")
    void crearCategoria_LanzaConflict_CuandoNombreYaExiste() {
        CategoriaRecordatorioCreateDTO dto = new CategoriaRecordatorioCreateDTO();
        dto.setNomCategoria("Existente");

        when(categoriaRepository.existsByNomCategoria("EXISTENTE")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> categoriaService.crearCategoria(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("Actualizar categoría - Éxito")
    void actualizarCategoria_Exito() {
        CategoriaRecordatorio cat = new CategoriaRecordatorio();
        cat.setIdCategoria(1);
        cat.setNomCategoria("VIEJA");
        
        CategoriaRecordatorioUpdateDTO dto = new CategoriaRecordatorioUpdateDTO();
        dto.setNomCategoria("NUEVA");

        when(categoriaRepository.findById(1)).thenReturn(Optional.of(cat));
        when(categoriaRepository.existsByNomCategoria("NUEVA")).thenReturn(false);
        when(categoriaRepository.save(any(CategoriaRecordatorio.class))).thenAnswer(i -> i.getArgument(0));

        CategoriaRecordatorioResponseDTO resultado = categoriaService.actualizarCategoria(1, dto);

        assertNotNull(resultado);
        assertEquals("NUEVA", resultado.getNomCategoria());
    }

    @Test
    @DisplayName("Actualizar categoría - Conflicto si el nuevo nombre ya existe (409)")
    void actualizarCategoria_LanzaConflict_CuandoNombreYaExiste() {
        CategoriaRecordatorio cat = new CategoriaRecordatorio();
        cat.setIdCategoria(1);
        cat.setNomCategoria("VIEJA");
        
        CategoriaRecordatorioUpdateDTO dto = new CategoriaRecordatorioUpdateDTO();
        dto.setNomCategoria("OTRA_EXISTENTE");

        when(categoriaRepository.findById(1)).thenReturn(Optional.of(cat));
        when(categoriaRepository.existsByNomCategoria("OTRA_EXISTENTE")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> categoriaService.actualizarCategoria(1, dto));
    }

    @Test
    @DisplayName("Dar de baja categoría - Éxito (Baja lógica)")
    void darDeBajaCategoria_Exito() {
        CategoriaRecordatorio cat = new CategoriaRecordatorio();
        cat.setIdCategoria(5);
        cat.setEstado(EstadoLogico.ACTIVO);

        when(categoriaRepository.findById(5)).thenReturn(Optional.of(cat));

        categoriaService.darDeBajaCategoria(5);

        assertEquals(EstadoLogico.INACTIVO, cat.getEstado());
        verify(categoriaRepository).save(cat);
    }
    
    @Test
    @DisplayName("Dar de baja categoría - No encontrada (404)")
    void darDeBajaCategoria_LanzaNotFound() {
        when(categoriaRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> categoriaService.darDeBajaCategoria(99));
    }
}
