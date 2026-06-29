package com.fides.sienep.service;

import com.fides.sienep.dto.ItrRequestDTO;
import com.fides.sienep.dto.ItrResponseDTO;
import com.fides.sienep.mapper.ItrMapper;
import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Itr;
import com.fides.sienep.model.ItrCarrera;
import com.fides.sienep.repository.CarreraRepository;
import com.fides.sienep.repository.ItrRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItrServiceTest {

    @Mock
    private ItrRepository itrRepository;

    @Mock
    private CarreraRepository carreraRepository;

    @Mock
    private ItrMapper itrMapper;

    @InjectMocks
    private ItrService itrService;

    @Test
    @DisplayName("crear: Lanza excepción si el nombre ya existe")
    void crear_LanzaExcepcion_SiNombreYaExiste() {
        ItrRequestDTO dto = new ItrRequestDTO();
        dto.setNomItr("Suroeste");

        when(itrRepository.existsByNomItr("Suroeste")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> itrService.crear(dto));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Ya existe un ITR con ese nombre.", exception.getReason());
        verify(itrRepository, never()).save(any(Itr.class));
    }

    @Test
    @DisplayName("crear: Crea ITR y asocia carreras correctamente")
    void crear_Exito_ConCarreras() {
        ItrRequestDTO dto = new ItrRequestDTO();
        dto.setNomItr("Centro-Sur");
        dto.setCarrerasIds(List.of(1));

        Itr itrGuardado1 = new Itr("Centro-Sur");
        itrGuardado1.setIdItr(1);

        Carrera carrera = new Carrera();
        carrera.setIdCarrera(1);
        carrera.setNomCarrera("LTI");

        ItrResponseDTO responseDTO = ItrResponseDTO.builder()
                .idItr(1)
                .nomItr("Centro-Sur")
                .build();

        when(itrRepository.existsByNomItr("Centro-Sur")).thenReturn(false);
        // Primer save (guarda ITR vacio)
        when(itrRepository.save(any(Itr.class))).thenReturn(itrGuardado1);
        when(carreraRepository.findById(1)).thenReturn(Optional.of(carrera));
        when(itrMapper.toDto(any(Itr.class))).thenReturn(responseDTO);

        ItrResponseDTO result = itrService.crear(dto);

        assertNotNull(result);
        assertEquals(1, result.getIdItr());
        verify(itrRepository, times(2)).save(any(Itr.class)); // 1 al crear, 1 al asociar
    }

    @Test
    @DisplayName("crear: Éxito sin carreras asociadas")
    void crear_Exito_SinCarreras() {
        ItrRequestDTO dto = new ItrRequestDTO();
        dto.setNomItr("Norte");
        // sin setCarrerasIds

        Itr itrGuardado = new Itr("Norte");
        itrGuardado.setIdItr(1);
        ItrResponseDTO responseDTO = ItrResponseDTO.builder().idItr(1).nomItr("Norte").build();

        when(itrRepository.existsByNomItr("Norte")).thenReturn(false);
        when(itrRepository.save(any(Itr.class))).thenReturn(itrGuardado);
        when(itrMapper.toDto(any(Itr.class))).thenReturn(responseDTO);

        ItrResponseDTO result = itrService.crear(dto);

        assertNotNull(result);
        verify(itrRepository, times(1)).save(any(Itr.class)); // solo 1 save, sin asociaciones
    }

    @Test
    @DisplayName("listar: Retorna lista de ITRs mapeados")
    void listar_Exito() {
        List<Itr> itrs = List.of(new Itr("Norte"));
        List<ItrResponseDTO> dtos = List.of(ItrResponseDTO.builder().build());

        when(itrRepository.findAll()).thenReturn(itrs);
        when(itrMapper.toDtoList(itrs)).thenReturn(dtos);

        List<ItrResponseDTO> result = itrService.listar();

        assertEquals(1, result.size());
        verify(itrRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("obtenerPorId: Lanza excepción si no se encuentra")
    void obtenerPorId_NoEncontrado() {
        when(itrRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> itrService.obtenerPorId(99));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("obtenerPorId: Retorna el DTO si se encuentra")
    void obtenerPorId_Exito() {
        Itr itr = new Itr("Norte");
        ItrResponseDTO dto = ItrResponseDTO.builder().build();

        when(itrRepository.findById(1)).thenReturn(Optional.of(itr));
        when(itrMapper.toDto(itr)).thenReturn(dto);

        ItrResponseDTO result = itrService.obtenerPorId(1);

        assertNotNull(result);
    }

    @Test
    @DisplayName("actualizar: Lanza excepción si ITR está inactivo")
    void actualizar_LanzaExcepcion_SiItrInactivo() {
        Itr itr = new Itr("Norte");
        itr.setEstado(EstadoLogico.INACTIVO);

        when(itrRepository.findById(1)).thenReturn(Optional.of(itr));

        ItrRequestDTO dto = new ItrRequestDTO();
        
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> itrService.actualizar(1, dto));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("No se puede modificar ni asociar carreras a un ITR inactivo.", exception.getReason());
    }

    @Test
    @DisplayName("actualizar: Lanza excepción si el nuevo nombre ya está en uso")
    void actualizar_LanzaExcepcion_SiNombreEnUso() {
        Itr itr = new Itr("Norte");
        itr.setIdItr(1);

        when(itrRepository.findById(1)).thenReturn(Optional.of(itr));
        when(itrRepository.existsByNomItr("Suroeste")).thenReturn(true);

        ItrRequestDTO dto = new ItrRequestDTO();
        dto.setNomItr("Suroeste");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> itrService.actualizar(1, dto));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("El nombre del ITR ya está en uso.", exception.getReason());
    }

    @Test
    @DisplayName("actualizar: Éxito con lógica de asociarCarreras (altas y bajas)")
    void actualizar_Exito_ConAsociacionCarreras() {
        Itr itr = new Itr("Norte");
        itr.setIdItr(1);
        
        Carrera c1 = new Carrera(); c1.setIdCarrera(1);
        Carrera c2 = new Carrera(); c2.setIdCarrera(2);
        
        ItrCarrera asociacionVieja = new ItrCarrera(itr, c1);
        asociacionVieja.setEstado(EstadoLogico.ACTIVO);
        itr.getCarreras().add(asociacionVieja);

        when(itrRepository.findById(1)).thenReturn(Optional.of(itr));
        // Vamos a cambiarle el nombre a "Suroeste" (no en uso)
        when(itrRepository.existsByNomItr("Suroeste")).thenReturn(false);
        // Vamos a pedir que asocie la carrera 2 (nueva) y quite la 1 (baja lógica)
        when(carreraRepository.findById(2)).thenReturn(Optional.of(c2));
        
        ItrRequestDTO dto = new ItrRequestDTO();
        dto.setNomItr("Suroeste");
        dto.setCarrerasIds(List.of(2)); // Viene la 2, desaparece la 1

        ItrResponseDTO responseMock = ItrResponseDTO.builder().build();
        when(itrRepository.save(itr)).thenReturn(itr);
        when(itrMapper.toDto(itr)).thenReturn(responseMock);

        ItrResponseDTO result = itrService.actualizar(1, dto);

        assertNotNull(result);
        assertEquals("Suroeste", itr.getNomItr());
        
        // Verificamos asociarCarreras internamente
        // La carrera 1 debió quedar inactiva
        assertEquals(EstadoLogico.INACTIVO, itr.getCarreras().get(0).getEstado());
        // La carrera 2 debió agregarse y estar activa
        assertEquals(2, itr.getCarreras().size());
        assertEquals(EstadoLogico.ACTIVO, itr.getCarreras().get(1).getEstado());
        assertEquals(2, itr.getCarreras().get(1).getCarrera().getIdCarrera());
    }

    @Test
    @DisplayName("actualizar: Lanza excepción si la nueva carrera a asociar no existe")
    void actualizar_CarreraInexistente() {
        Itr itr = new Itr("Norte");
        when(itrRepository.findById(1)).thenReturn(Optional.of(itr));
        
        ItrRequestDTO dto = new ItrRequestDTO();
        dto.setNomItr("Norte");
        dto.setCarrerasIds(List.of(99)); // Carrera inexistente

        when(carreraRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> itrService.actualizar(1, dto));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Carrera con ID 99 no encontrada.", exception.getReason());
    }

    @Test
    @DisplayName("darDeBaja: Modifica el estado a INACTIVO")
    void darDeBaja_Exito() {
        Itr itr = new Itr("Norte");
        when(itrRepository.findById(1)).thenReturn(Optional.of(itr));

        itrService.darDeBaja(1);

        assertEquals(EstadoLogico.INACTIVO, itr.getEstado());
        verify(itrRepository, times(1)).save(itr);
    }

    @Test
    @DisplayName("reactivar: Modifica el estado a ACTIVO")
    void reactivar_Exito() {
        Itr itr = new Itr("Norte");
        itr.setEstado(EstadoLogico.INACTIVO);
        when(itrRepository.findById(1)).thenReturn(Optional.of(itr));

        itrService.reactivar(1);

        assertEquals(EstadoLogico.ACTIVO, itr.getEstado());
        verify(itrRepository, times(1)).save(itr);
    }
}
