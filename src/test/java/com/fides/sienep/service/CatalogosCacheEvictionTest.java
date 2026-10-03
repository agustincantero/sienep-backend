package com.fides.sienep.service;

import com.fides.sienep.dto.CarreraRequestDTO;
import com.fides.sienep.dto.ItrRequestDTO;
import com.fides.sienep.mapper.CarreraMapper;
import com.fides.sienep.mapper.ItrMapper;
import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Itr;
import com.fides.sienep.repository.CarreraRepository;
import com.fides.sienep.repository.ItrRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * GET /carreras incluye los ITRs de cada carrera y GET /itrs incluye los nombres de sus carreras,
 * así que una escritura en cualquiera de los dos tiene que vaciar los dos cachés. Los tests con
 * Mockito puro no lo detectan porque no pasan por el proxy de caché: acá se levanta un contexto
 * mínimo con @EnableCaching y los dos services reales (repositorios y mappers simulados).
 * Sin @EnableMethodSecurity ni transacciones: los @PreAuthorize y @Transactional no aplican.
 */
@SpringJUnitConfig(CatalogosCacheEvictionTest.Config.class)
class CatalogosCacheEvictionTest {

    @Configuration
    @EnableCaching
    static class Config {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("carreras", "itrs");
        }

        @Bean
        ItrRepository itrRepository() {
            return mock(ItrRepository.class);
        }

        @Bean
        CarreraRepository carreraRepository() {
            return mock(CarreraRepository.class);
        }

        @Bean
        ItrMapper itrMapper() {
            return mock(ItrMapper.class);
        }

        @Bean
        CarreraMapper carreraMapper() {
            return mock(CarreraMapper.class);
        }

        @Bean
        ItrService itrService(ItrRepository itrRepository, CarreraRepository carreraRepository, ItrMapper itrMapper) {
            return new ItrService(itrRepository, carreraRepository, itrMapper);
        }

        @Bean
        CarreraService carreraService(CarreraRepository carreraRepository, CarreraMapper carreraMapper) {
            return new CarreraService(carreraRepository, carreraMapper);
        }
    }

    @Autowired private CacheManager cacheManager;
    @Autowired private ItrService itrService;
    @Autowired private CarreraService carreraService;
    @Autowired private ItrRepository itrRepository;
    @Autowired private CarreraRepository carreraRepository;

    private Cache carreras;
    private Cache itrs;

    @BeforeEach
    void setUp() {
        reset(itrRepository, carreraRepository);
        carreras = cacheManager.getCache("carreras");
        itrs = cacheManager.getCache("itrs");
        // Simula un listado ya cacheado en cada caja.
        carreras.put(EstadoLogico.ACTIVO, List.of());
        itrs.put(EstadoLogico.ACTIVO, List.of());
    }

    private Itr itrActivo() {
        Itr itr = new Itr("Norte");
        itr.setIdItr(1);
        return itr;
    }

    private Carrera carreraActiva() {
        Carrera carrera = new Carrera();
        carrera.setIdCarrera(1);
        carrera.setNomCarrera("LTI");
        carrera.setEstado(EstadoLogico.ACTIVO);
        return carrera;
    }

    @Test
    @DisplayName("Editar un ITR (asociar carreras) vacía también el caché de carreras")
    void editarItr_VaciaCacheDeCarreras() {
        when(itrRepository.findById(1)).thenReturn(Optional.of(itrActivo()));
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carreraActiva()));
        when(itrRepository.save(any(Itr.class))).thenAnswer(inv -> inv.getArgument(0));

        ItrRequestDTO dto = new ItrRequestDTO();
        dto.setNomItr("Norte");
        dto.setCarrerasIds(List.of(1));
        itrService.editar(1, dto);

        assertNull(carreras.get(EstadoLogico.ACTIVO), "GET /carreras seguiría sin mostrar el ITR recién asociado");
        assertNull(itrs.get(EstadoLogico.ACTIVO));
    }

    @Test
    @DisplayName("Desactivar un ITR vacía también el caché de carreras")
    void desactivarItr_VaciaCacheDeCarreras() {
        when(itrRepository.findById(1)).thenReturn(Optional.of(itrActivo()));

        itrService.desactivar(1);

        assertNull(carreras.get(EstadoLogico.ACTIVO));
        assertNull(itrs.get(EstadoLogico.ACTIVO));
    }

    @Test
    @DisplayName("Editar una carrera (renombrar) vacía también el caché de ITRs")
    void editarCarrera_VaciaCacheDeItrs() {
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carreraActiva()));
        when(carreraRepository.save(any(Carrera.class))).thenAnswer(inv -> inv.getArgument(0));

        CarreraRequestDTO dto = new CarreraRequestDTO();
        dto.setNomCarrera("Licenciatura en TI");
        carreraService.editar(1, dto);

        assertNull(itrs.get(EstadoLogico.ACTIVO), "GET /itrs seguiría mostrando el nombre viejo de la carrera");
        assertNull(carreras.get(EstadoLogico.ACTIVO));
    }

    @Test
    @DisplayName("Desactivar una carrera vacía también el caché de ITRs")
    void desactivarCarrera_VaciaCacheDeItrs() {
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carreraActiva()));

        carreraService.desactivar(1);

        assertNull(itrs.get(EstadoLogico.ACTIVO));
        assertNull(carreras.get(EstadoLogico.ACTIVO));
    }
}
