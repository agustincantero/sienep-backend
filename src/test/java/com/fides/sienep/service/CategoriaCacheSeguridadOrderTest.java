package com.fides.sienep.service;

import com.fides.sienep.dto.CategoriaInstanciaResponseDTO;
import com.fides.sienep.mapper.CategoriaInstanciaMapper;
import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.CategoriaInstanciaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

// B8 — @Cacheable y @PreAuthorize son completamente inertes bajo Mockito puro
// (@InjectMocks nunca pasa por un proxy de AOP), así que el orden real entre
// ambos solo se puede observar con un contexto de Spring de verdad. Por eso este
// es el único test de todo el proyecto que usa @SpringBootTest para un service.
//
// Por qué importa: si @Cacheable se aplicara "por afuera" de @PreAuthorize (cache
// hit antes que el chequeo de seguridad), un usuario SIN el permiso podría recibir
// una respuesta cacheada por otro usuario CON permiso, sin pasar nunca por el
// chequeo de autorización — un bypass real, no solo una cuestión de estilo.
// Repositorio y mapper se mockean para no depender de datos reales; el resto del
// contexto (incluidos AuditoriaAspect y el CacheManager real) queda intacto.
@SpringBootTest
@ActiveProfiles("test")
class CategoriaCacheSeguridadOrderTest {

    @Autowired
    private CategoriaInstanciaService categoriaInstanciaService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private CategoriaInstanciaRepository categoriaRepository;

    @MockitoBean
    private CategoriaInstanciaMapper categoriaMapper;

    @AfterEach
    void limpiarContextoYCache() {
        SecurityContextHolder.clearContext();
        if (cacheManager.getCache("categoriasInstancia") != null) {
            cacheManager.getCache("categoriasInstancia").clear();
        }
    }

    @Test
    void sinPermiso_NoRecibeElValorCacheadoPorOtroUsuarioConPermiso() {
        CategoriaInstancia categoria = new CategoriaInstancia();
        categoria.setIdCategoria(1);
        categoria.setNomCategoria("REUNION");
        categoria.setEstado(EstadoLogico.ACTIVO);

        CategoriaInstanciaResponseDTO dto = new CategoriaInstanciaResponseDTO();
        dto.setIdCategoria(1);
        dto.setNomCategoria("REUNION");
        dto.setEstado(EstadoLogico.ACTIVO);

        when(categoriaRepository.buscarConFiltros(null, null)).thenReturn(List.of(categoria));
        when(categoriaMapper.toDtoList(List.of(categoria))).thenReturn(List.of(dto));

        // 1) Usuario CON el permiso: pasa el chequeo y puebla la caché
        autenticarCon("VER_CATEGORIA_INSTANCIA");
        List<CategoriaInstanciaResponseDTO> primero = categoriaInstanciaService.obtenerTodas(null, null);
        assertEquals(1, primero.size());

        // 2) Usuario SIN el permiso, misma clave de caché (mismos parámetros: null, null).
        // Si @PreAuthorize corre por fuera de @Cacheable (como corresponde), este
        // llamado debe seguir pasando por el chequeo de seguridad y ser rechazado,
        // sin importar que la respuesta ya esté en caché.
        autenticarCon("ALGUN_OTRO_PERMISO");
        assertThrows(AccessDeniedException.class,
                () -> categoriaInstanciaService.obtenerTodas(null, null));
    }

    private void autenticarCon(String permiso) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "1", null, List.of(new SimpleGrantedAuthority(permiso))));
    }
}
