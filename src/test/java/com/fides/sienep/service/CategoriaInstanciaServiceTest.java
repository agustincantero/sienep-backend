package com.fides.sienep.service;

import com.fides.sienep.dto.CategoriaInstanciaCreateDTO;
import com.fides.sienep.dto.CategoriaInstanciaResponseDTO;
import com.fides.sienep.dto.CategoriaInstanciaUpdateDTO;
import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.CategoriaInstanciaRepository;
import org.junit.jupiter.api.DisplayName;
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

    // ── Helpers ───────────────────────────────────────────────

    private CategoriaInstancia categoriaActiva(int id, String nombre) {
        CategoriaInstancia cat = new CategoriaInstancia();
        cat.setIdCategoria(id);
        cat.setNomCategoria(nombre);
        cat.setEstado(EstadoLogico.ACTIVO);
        return cat;
    }

    private CategoriaInstancia categoriaInactiva(int id, String nombre) {
        CategoriaInstancia cat = new CategoriaInstancia();
        cat.setIdCategoria(id);
        cat.setNomCategoria(nombre);
        cat.setEstado(EstadoLogico.INACTIVO);
        return cat;
    }

    // ── Listar ────────────────────────────────────────────────

    @Test
    @DisplayName("Listar categorías de instancia - Éxito")
    void obtenerTodas_DebeRetornarListaDeCategorias() {
        CategoriaInstancia cat = categoriaActiva(1, "CAT_1");
        when(categoriaRepository.buscarConFiltros("CAT", EstadoLogico.ACTIVO)).thenReturn(List.of(cat));

        List<CategoriaInstanciaResponseDTO> resultado = categoriaService.obtenerTodas("CAT", EstadoLogico.ACTIVO);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("CAT_1", resultado.get(0).getNomCategoria());
    }

    // ── Obtener por ID ────────────────────────────────────────

    @Test
    @DisplayName("Obtener categoría por ID - Éxito")
    void obtenerPorId_Exito() {
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(categoriaActiva(1, "CAT_1")));

        CategoriaInstanciaResponseDTO resultado = categoriaService.obtenerPorId(1);

        assertNotNull(resultado);
        assertEquals("CAT_1", resultado.getNomCategoria());
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
    @DisplayName("Crear categoría - Conflicto si nombre ya existe (409)")
    void crearCategoria_LanzaConflict_CuandoNombreYaExiste() {
        CategoriaInstanciaCreateDTO dto = new CategoriaInstanciaCreateDTO();
        dto.setNomCategoria("Existente");

        when(categoriaRepository.existsByNomCategoria("EXISTENTE")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoriaService.crearCategoria(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ── Actualizar ────────────────────────────────────────────

    @Test
    @DisplayName("Actualizar categoría - Éxito")
    void actualizarCategoria_Exito() {
        CategoriaInstanciaUpdateDTO dto = new CategoriaInstanciaUpdateDTO();
        dto.setNomCategoria("NUEVA");

        when(categoriaRepository.findById(1)).thenReturn(Optional.of(categoriaActiva(1, "VIEJA")));
        when(categoriaRepository.existsByNomCategoria("NUEVA")).thenReturn(false);
        when(categoriaRepository.save(any(CategoriaInstancia.class))).thenAnswer(i -> i.getArgument(0));

        CategoriaInstanciaResponseDTO resultado = categoriaService.actualizarCategoria(1, dto);

        assertNotNull(resultado);
        assertEquals("NUEVA", resultado.getNomCategoria());
    }

    @Test
    @DisplayName("Actualizar categoría - Conflicto si el nuevo nombre ya existe (409)")
    void actualizarCategoria_LanzaConflict_CuandoNombreYaExiste() {
        CategoriaInstanciaUpdateDTO dto = new CategoriaInstanciaUpdateDTO();
        dto.setNomCategoria("OTRA_EXISTENTE");

        when(categoriaRepository.findById(1)).thenReturn(Optional.of(categoriaActiva(1, "VIEJA")));
        when(categoriaRepository.existsByNomCategoria("OTRA_EXISTENTE")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoriaService.actualizarCategoria(1, dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    @DisplayName("Actualizar categoría - No se puede editar una categoría inactiva (409)")
    void actualizarCategoria_LanzaConflict_CuandoEstaInactiva() {
        CategoriaInstanciaUpdateDTO dto = new CategoriaInstanciaUpdateDTO();
        dto.setNomCategoria("CUALQUIER_NOMBRE");

        when(categoriaRepository.findById(1)).thenReturn(Optional.of(categoriaInactiva(1, "VIEJA")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoriaService.actualizarCategoria(1, dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ── Desactivar ────────────────────────────────────────────

    @Test
    @DisplayName("Desactivar categoría - Éxito (baja lógica)")
    void desactivarCategoria_Exito() {
        CategoriaInstancia cat = categoriaActiva(5, "REUNIÓN");
        when(categoriaRepository.findById(5)).thenReturn(Optional.of(cat));
        when(categoriaRepository.existeInstanciaActivaConCategoria(5)).thenReturn(false);

        categoriaService.desactivarCategoria(5);

        assertEquals(EstadoLogico.INACTIVO, cat.getEstado());
        verify(categoriaRepository).save(cat);
    }

    @Test
    @DisplayName("Desactivar categoría - No encontrada (404)")
    void desactivarCategoria_LanzaNotFound() {
        when(categoriaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> categoriaService.desactivarCategoria(99));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desactivar categoría - Ya estaba inactiva (409)")
    void desactivarCategoria_LanzaConflict_CuandoYaEstaInactiva() {
        when(categoriaRepository.findById(5)).thenReturn(Optional.of(categoriaInactiva(5, "REUNIÓN")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoriaService.desactivarCategoria(5));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desactivar categoría - Tiene instancias activas asociadas (409)")
    void desactivarCategoria_LanzaConflict_CuandoTieneInstanciasActivas() {
        when(categoriaRepository.findById(5)).thenReturn(Optional.of(categoriaActiva(5, "REUNIÓN")));
        when(categoriaRepository.existeInstanciaActivaConCategoria(5)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoriaService.desactivarCategoria(5));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(categoriaRepository, never()).save(any());
    }

    // ── Reactivar ─────────────────────────────────────────────

    @Test
    @DisplayName("Reactivar categoría - Éxito")
    void reactivarCategoria_Exito() {
        CategoriaInstancia cat = categoriaInactiva(5, "REUNIÓN");
        when(categoriaRepository.findById(5)).thenReturn(Optional.of(cat));
        when(categoriaRepository.save(any(CategoriaInstancia.class))).thenAnswer(i -> i.getArgument(0));

        CategoriaInstanciaResponseDTO resultado = categoriaService.reactivarCategoria(5);

        assertEquals(EstadoLogico.ACTIVO, resultado.getEstado());
        verify(categoriaRepository).save(cat);
    }

    @Test
    @DisplayName("Reactivar categoría - No encontrada (404)")
    void reactivarCategoria_LanzaNotFound() {
        when(categoriaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> categoriaService.reactivarCategoria(99));
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar categoría - Ya estaba activa (409)")
    void reactivarCategoria_LanzaConflict_CuandoYaEstaActiva() {
        when(categoriaRepository.findById(5)).thenReturn(Optional.of(categoriaActiva(5, "REUNIÓN")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> categoriaService.reactivarCategoria(5));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(categoriaRepository, never()).save(any());
    }
}