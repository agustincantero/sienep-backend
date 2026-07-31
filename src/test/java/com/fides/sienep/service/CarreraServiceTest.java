package com.fides.sienep.service;

import com.fides.sienep.dto.CarreraRequestDTO;
import com.fides.sienep.dto.CarreraResponseDTO;
import com.fides.sienep.mapper.CarreraMapper;
import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.repository.CarreraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarreraServiceTest {

    @Mock
    private CarreraRepository carreraRepository;

    @Mock
    private CarreraMapper carreraMapper;

    @InjectMocks
    private CarreraService carreraService;

    private Carrera carreraMock;
    private CarreraRequestDTO requestDTO;
    private CarreraResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        carreraMock = new Carrera();
        carreraMock.setIdCarrera(1);
        carreraMock.setNomCarrera("Ingeniería en Sistemas");
        carreraMock.setEstado(EstadoLogico.ACTIVO);

        requestDTO = new CarreraRequestDTO();
        requestDTO.setNomCarrera("Ingeniería en Sistemas");

        responseDTO = CarreraResponseDTO.builder()
                .idCarrera(1)
                .nomCarrera("Ingeniería en Sistemas")
                .estado(EstadoLogico.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("Crear carrera - Éxito")
    void crear_Exito() {
        when(carreraRepository.existsByNomCarrera(anyString())).thenReturn(false);
        when(carreraRepository.save(any(Carrera.class))).thenReturn(carreraMock);
        when(carreraMapper.toDto(any(Carrera.class))).thenReturn(responseDTO);

        CarreraResponseDTO result = carreraService.crear(requestDTO);

        assertNotNull(result);
        assertEquals(responseDTO.getNomCarrera(), result.getNomCarrera());
        verify(carreraRepository).existsByNomCarrera(requestDTO.getNomCarrera());
        verify(carreraRepository).save(any(Carrera.class));
    }

    @Test
    @DisplayName("Crear carrera - Conflicto si ya existe")
    void crear_LanzaExcepcion_SiYaExiste() {
        when(carreraRepository.existsByNomCarrera(anyString())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> carreraService.crear(requestDTO));
        
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(carreraRepository, never()).save(any(Carrera.class));
    }

    @Test
    @DisplayName("Obtener por ID - Éxito")
    void obtenerPorId_Exito() {
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carreraMock));
        when(carreraMapper.toDto(carreraMock)).thenReturn(responseDTO);

        CarreraResponseDTO result = carreraService.obtenerPorId(1);

        assertNotNull(result);
        assertEquals(1, result.getIdCarrera());
    }

    @Test
    @DisplayName("Obtener por ID - No encontrada")
    void obtenerPorId_LanzaExcepcion_NoEncontrada() {
        when(carreraRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> carreraService.obtenerPorId(99));
        
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Listar - Éxito")
    void listar_Exito() {
        List<Carrera> carreras = List.of(carreraMock);
        List<CarreraResponseDTO> responseDTOs = List.of(responseDTO);

        when(carreraRepository.findAll()).thenReturn(carreras);
        when(carreraMapper.toDtoList(carreras)).thenReturn(responseDTOs);

        List<CarreraResponseDTO> result = carreraService.listar();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Actualizar carrera - Éxito")
    void actualizar_Exito() {
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carreraMock));
        when(carreraRepository.save(any(Carrera.class))).thenReturn(carreraMock);
        when(carreraMapper.toDto(any(Carrera.class))).thenReturn(responseDTO);

        requestDTO.setNomCarrera("Nuevo Nombre");
        CarreraResponseDTO result = carreraService.actualizar(1, requestDTO);

        assertNotNull(result);
        assertEquals("Nuevo Nombre", carreraMock.getNomCarrera()); // Verificamos que se seteó en la entidad
        verify(carreraRepository).save(carreraMock);
    }

    @Test
    @DisplayName("Actualizar carrera - Rechaza editar una carrera inactiva (M3)")
    void actualizar_LanzaConflicto_CarreraInactiva() {
        carreraMock.setEstado(EstadoLogico.INACTIVO);
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carreraMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> carreraService.actualizar(1, requestDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(carreraRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar carrera - No encontrada")
    void actualizar_LanzaExcepcion_NoEncontrada() {
        when(carreraRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> carreraService.actualizar(99, requestDTO));
        
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(carreraRepository, never()).save(any());
    }

    @Test
    @DisplayName("Eliminar (Baja Lógica) - Éxito")
    void eliminar_Exito() {
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carreraMock));
        when(carreraRepository.save(any(Carrera.class))).thenReturn(carreraMock);

        carreraService.eliminar(1);

        assertEquals(EstadoLogico.INACTIVO, carreraMock.getEstado()); // Comprueba que cambió el estado
        verify(carreraRepository).save(carreraMock);
    }

    @Test
    @DisplayName("Reactivar - Éxito")
    void reactivar_Exito() {
        carreraMock.setEstado(EstadoLogico.INACTIVO);
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carreraMock));
        when(carreraRepository.save(any(Carrera.class))).thenReturn(carreraMock);

        carreraService.reactivar(1);

        assertEquals(EstadoLogico.ACTIVO, carreraMock.getEstado()); // Comprueba que lo volvió a poner en ACTIVO
        verify(carreraRepository).save(carreraMock);
    }
}
