package com.fides.sienep.service;

import com.fides.sienep.dto.CategoriaRecordatorioRequestDTO;
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

    // ── Helpers ───────────────────────────────────────────────

    private CategoriaRecordatorio categoriaActiva(int id, String nombre) {
        CategoriaRecordatorio cat = new CategoriaRecordatorio();
        cat.setIdCategoria(id);
        cat.setNomCategoria(nombre);
        cat.setEstado(EstadoLogico.ACTIVO);
        return cat;
    }

    private CategoriaRecordatorio categoriaInactiva(int id, String nombre) {
        CategoriaRecordatorio cat = new CategoriaRecordatorio();
        cat.setIdCategoria(id);
        cat.setNomCategoria(nombre);
        cat.setEstado(EstadoLogico.INACTIVO);
        return cat;
    }

    // ── Listar ────────────────────────────────────────────────

    @Test
    @DisplayName("Listar categorías de recordatorio - Con estado explícito")
    void listar_DebeRetornarListaDeCategorias() {
        CategoriaRecordatorio cat = categoriaActiva(1, "CAT_REC");
        when(categoriaRepository.buscarConFiltros("CAT", EstadoLogico.ACTIVO)).thenReturn(List.of(cat));

        List<CategoriaRecordatorioResponseDTO> resultado = categoriaService.listar("CAT", EstadoLogico.ACTIVO);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("CAT_REC", resultado.get(0).getNomCategoria());
    }

    @Test
    @DisplayName("Listar categorías de recordatorio - Sin estado, devuelve solo ACTIVAS")
    void listar_SinEstado_DevuelveSoloActivas() {
        CategoriaRecordatorio cat = categoriaActiva(1, "CAT_REC");
        when(categoriaRepository.buscarConFiltros(null, EstadoLogico.ACTIVO)).thenReturn(List.of(cat));

        List<CategoriaRecordatorioResponseDTO> resultado = categoriaService.listar(null, null);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(categoriaRepository).buscarConFiltros(null, EstadoLogico.ACTIVO);
    }

    // ── Obtener por ID ────────────────────────────────────────

    @Test
    @DisplayName("Obtener categoría por ID - Éxito")
    void obtenerPorId_Exito() {
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(categoriaActiva(1, "CAT_REC")));

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

    // ── Crear ─────────────────────────────────────────────────

    @Test
    @DisplayName("Crear categoría - Éxito")
    void crear_Exito() {
        CategoriaRecordatorioRequestDTO dto = new CategoriaRecordatorioRequestDTO();
        dto.setNomCategoria("  Categoria Nueva  ");
        dto.setDescripcion("Una descripcion");

        when(categoriaRepository.existsByNomCategoria("CATEGORIA_NUEVA")).thenReturn(false);
        when(categoriaRepository.save(any(CategoriaRecordatorio.class))).thenAnswer(invocation -> {
            CategoriaRecordatorio guardada = invocation.getArgument(0);
            guardada.setIdCategoria(1);
            return guardada;
        });

        CategoriaRecordatorioResponseDTO resultado = categoriaService.crear(dto);

        assertNotNull(resultado);
        assertEquals(1, resultado.getIdCategoria());
        assertEquals("CATEGORIA_NUEVA", resultado.getNomCategoria());
        assertEquals(EstadoLogico.ACTIVO, resultado.getEstado());
    }

    @Test
    @DisplayName("Crear categoría - Conflicto si nombre ya existe (409)")
    void crear_LanzaConflict_CuandoNombreYaExiste() {
        CategoriaRecordatorioRequestDTO dto = new CategoriaRecordatorioRequestDTO();
        dto.setNomCategoria("Existente");

        when(categoriaRepository.existsByNomCategoria("EXISTENTE")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoriaService.crear(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ── Editar ────────────────────────────────────────────

    @Test
    @DisplayName("Editar categoría - Éxito")
    void editar_Exito() {
        CategoriaRecordatorioUpdateDTO dto = new CategoriaRecordatorioUpdateDTO();
        dto.setNomCategoria("NUEVA");

        when(categoriaRepository.findById(1)).thenReturn(Optional.of(categoriaActiva(1, "VIEJA")));
        when(categoriaRepository.existsByNomCategoria("NUEVA")).thenReturn(false);
        when(categoriaRepository.save(any(CategoriaRecordatorio.class))).thenAnswer(i -> i.getArgument(0));

        CategoriaRecordatorioResponseDTO resultado = categoriaService.editar(1, dto);

        assertNotNull(resultado);
        assertEquals("NUEVA", resultado.getNomCategoria());
    }

    @Test
    @DisplayName("Editar categoría - Conflicto si el nuevo nombre ya existe (409)")
    void editar_LanzaConflict_CuandoNombreYaExiste() {
        CategoriaRecordatorioUpdateDTO dto = new CategoriaRecordatorioUpdateDTO();
        dto.setNomCategoria("OTRA_EXISTENTE");

        when(categoriaRepository.findById(1)).thenReturn(Optional.of(categoriaActiva(1, "VIEJA")));
        when(categoriaRepository.existsByNomCategoria("OTRA_EXISTENTE")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoriaService.editar(1, dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("Editar categoría - No se puede editar una categoría inactiva (409)")
    void editar_LanzaConflict_CuandoEstaInactiva() {
        CategoriaRecordatorioUpdateDTO dto = new CategoriaRecordatorioUpdateDTO();
        dto.setNomCategoria("CUALQUIER_NOMBRE");

        when(categoriaRepository.findById(1)).thenReturn(Optional.of(categoriaInactiva(1, "VIEJA")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoriaService.editar(1, dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ── Desactivar ────────────────────────────────────────────

    @Test
    @DisplayName("Desactivar categoría - Éxito (baja lógica)")
    void desactivar_Exito() {
        CategoriaRecordatorio cat = categoriaActiva(5, "SEGUIMIENTO");
        when(categoriaRepository.findById(5)).thenReturn(Optional.of(cat));
        when(categoriaRepository.existeRecordatorioActivoConCategoria(5)).thenReturn(false);

        categoriaService.desactivar(5);

        assertEquals(EstadoLogico.INACTIVO, cat.getEstado());
        verify(categoriaRepository).save(cat);
    }

    @Test
    @DisplayName("Desactivar categoría - No encontrada (404)")
    void desactivar_LanzaNotFound() {
        when(categoriaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> categoriaService.desactivar(99));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desactivar categoría - Ya estaba inactiva (409)")
    void desactivar_LanzaConflict_CuandoYaEstaInactiva() {
        when(categoriaRepository.findById(5)).thenReturn(Optional.of(categoriaInactiva(5, "SEGUIMIENTO")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoriaService.desactivar(5));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desactivar categoría - Tiene recordatorios activos asociados (409)")
    void desactivar_LanzaConflict_CuandoTieneRecordatoriosActivos() {
        when(categoriaRepository.findById(5)).thenReturn(Optional.of(categoriaActiva(5, "SEGUIMIENTO")));
        when(categoriaRepository.existeRecordatorioActivoConCategoria(5)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoriaService.desactivar(5));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(categoriaRepository, never()).save(any());
    }

    // ── Reactivar ─────────────────────────────────────────────

    @Test
    @DisplayName("Reactivar categoría - Éxito")
    void reactivar_Exito() {
        CategoriaRecordatorio cat = categoriaInactiva(5, "SEGUIMIENTO");
        when(categoriaRepository.findById(5)).thenReturn(Optional.of(cat));
        when(categoriaRepository.save(any(CategoriaRecordatorio.class))).thenAnswer(i -> i.getArgument(0));

        CategoriaRecordatorioResponseDTO resultado = categoriaService.reactivar(5);

        assertEquals(EstadoLogico.ACTIVO, resultado.getEstado());
        verify(categoriaRepository).save(cat);
    }

    @Test
    @DisplayName("Reactivar categoría - No encontrada (404)")
    void reactivar_LanzaNotFound() {
        when(categoriaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> categoriaService.reactivar(99));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar categoría - Ya estaba activa (409)")
    void reactivar_LanzaConflict_CuandoYaEstaActiva() {
        when(categoriaRepository.findById(5)).thenReturn(Optional.of(categoriaActiva(5, "SEGUIMIENTO")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoriaService.reactivar(5));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(categoriaRepository, never()).save(any());
    }
}