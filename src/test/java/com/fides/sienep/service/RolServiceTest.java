package com.fides.sienep.service;

import com.fides.sienep.dto.RolCreateDTO;
import com.fides.sienep.dto.RolResponseDTO;
import com.fides.sienep.dto.RolUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.PermisoRepository;
import com.fides.sienep.repository.RolRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;
import com.fides.sienep.mapper.RolMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolServiceTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PermisoRepository permisoRepository;

    @Mock
    private EntityManager entityManager;

    @SuppressWarnings("unused")
    @Spy
    private RolMapper mapper = Mappers.getMapper(RolMapper.class);

    @InjectMocks
    private RolService rolService;

    private Rol rolExistente;

    @BeforeEach
    void setUp() {
        // Configuramos un rol de prueba que usaremos en varios tests
        rolExistente = new Rol();
        rolExistente.setIdRol(1);
        rolExistente.setNombre("TUTOR");
        rolExistente.setDescripcion("Tutor de prueba");
        rolExistente.setEstado(EstadoLogico.ACTIVO);
    }

    @Test
    void obtenerPorId_DebeRetornarDTO_CuandoElRolExiste() {
        // Arrange (Preparación): Le decimos al repositorio falso cómo debe comportarse
        when(rolRepository.findById(1)).thenReturn(Optional.of(rolExistente));

        // Act (Ejecución)
        RolResponseDTO resultado = rolService.obtenerPorId(1);

        // Assert (Verificación)
        assertNotNull(resultado);
        assertEquals("TUTOR", resultado.getNombre());
        verify(rolRepository, times(1)).findById(1); // Verificamos que se llamó a la BD 1 vez
    }

    @Test
    void obtenerPorId_DebeLanzarExcepcion_CuandoElRolNoExiste() {
        // Arrange
        when(rolRepository.findById(99)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            rolService.obtenerPorId(99);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Rol no encontrado", exception.getReason());
    }

    @Test
    void crearRol_DebeGuardarYRetornarDTO_CuandoDatosSonValidos() {
        // Arrange
        RolCreateDTO requestDTO = new RolCreateDTO();
        requestDTO.setNombre("nuevo rol  "); // Lo mandamos con espacios y minúsculas
        requestDTO.setDescripcion("Rol nuevo");
        requestDTO.setPermisosIds(List.of(1, 2));

        // Simulamos que el nombre limpio no existe en la BD
        when(rolRepository.existsByNombre("NUEVO_ROL")).thenReturn(false);
        // Simulamos que los 2 permisos existen
        when(permisoRepository.countByIdPermisoIn(requestDTO.getPermisosIds())).thenReturn(2L);
        // Simulamos el guardado
        when(rolRepository.save(any(Rol.class))).thenAnswer(invocation -> {
            Rol rolGuardado = invocation.getArgument(0);
            rolGuardado.setIdRol(5); // Simulamos que la BD le asignó el ID 5
            return rolGuardado;
        });

        // Simulamos que el EntityManager encuentra los permisos
        Permiso permisoFalso = new Permiso();
        when(entityManager.getReference(eq(Permiso.class), anyInt())).thenReturn(permisoFalso);

        // Act
        RolResponseDTO resultado = rolService.crearRol(requestDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals("NUEVO_ROL", resultado.getNombre()); // Verificamos que limpió el nombre
        assertEquals(EstadoLogico.ACTIVO, resultado.getEstado()); // Verificamos que forzó a ACTIVO
        verify(rolRepository).save(any(Rol.class));
    }

    @Test
    void crearRol_DebeLanzarExcepcion_CuandoNombreYaExiste() {
        // Arrange
        RolCreateDTO requestDTO = new RolCreateDTO();
        requestDTO.setNombre("TUTOR");

        when(rolRepository.existsByNombre("TUTOR")).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            rolService.crearRol(requestDTO);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("El nombre del rol ya existe", exception.getReason());
        verify(rolRepository, never()).save(any(Rol.class)); // Verificamos que nunca intentó guardar
    }

    @Test
    void darDeBajaRol_DebeLanzarExcepcion_SiEsAdministrador() {
        // Arrange
        Rol admin = new Rol();
        admin.setIdRol(1);
        admin.setNombre("ADMINISTRADOR");
        
        when(rolRepository.findById(1)).thenReturn(Optional.of(admin));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            rolService.darDeBajaRol(1);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Acción denegada"));
        verify(rolRepository, never()).save(any(Rol.class));
    }

    @Test
    void darDeBajaRol_DebeDarDeBajaCorrectamente_CuandoEsRolValido() {
        // Arrange
        when(rolRepository.findById(1)).thenReturn(Optional.of(rolExistente));
        
        // Act
        rolService.darDeBajaRol(1);
        
        // Assert
        assertEquals(EstadoLogico.INACTIVO, rolExistente.getEstado());
        verify(rolRepository, times(1)).save(rolExistente);
    }

    @Test
    void obtenerTodos_DebeRetornarListaDeDTOs() {
        // Arrange
        when(rolRepository.findAll()).thenReturn(List.of(rolExistente));

        // Act
        List<RolResponseDTO> resultado = rolService.obtenerTodos();

        // Assert
        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
        assertEquals("TUTOR", resultado.get(0).getNombre());
    }

    @Test
    void actualizarRol_DebeActualizarDatosYManejarPermisosCorrectamente() {
        // Arrange
        RolUpdateDTO updateDTO = new RolUpdateDTO();
        updateDTO.setNombre("TUTOR_MODIFICADO");
        updateDTO.setDescripcion("Nueva desc");
        updateDTO.setEstado(EstadoLogico.ACTIVO);
        updateDTO.setPermisosIds(List.of(2)); // Le mandamos solo el permiso 2 (quitando el 1 que tenía)

        // Simulamos que el rol ya tenía el permiso 1 activo
        Permiso permiso1 = new Permiso("PERMISO_1", "Desc");
        permiso1.setIdPermiso(1);
        com.fides.sienep.model.RolPermiso rpExistente = new com.fides.sienep.model.RolPermiso(rolExistente, permiso1, EstadoLogico.ACTIVO);
        rolExistente.getRolPermisos().add(rpExistente);

        when(rolRepository.findById(1)).thenReturn(Optional.of(rolExistente));
        when(rolRepository.existsByNombre("TUTOR_MODIFICADO")).thenReturn(false); // El nuevo nombre es válido
        when(permisoRepository.countByIdPermisoIn(updateDTO.getPermisosIds())).thenReturn(1L); // El permiso 2 existe
        
        Permiso permiso2Falso = new Permiso();
        permiso2Falso.setIdPermiso(2);
        when(entityManager.getReference(eq(Permiso.class), eq(2))).thenReturn(permiso2Falso);
        
        when(rolRepository.save(any(Rol.class))).thenReturn(rolExistente);

        // Act
        RolResponseDTO resultado = rolService.actualizarRol(1, updateDTO);

        // Assert
        assertNotNull(resultado);
        assertEquals("TUTOR_MODIFICADO", resultado.getNombre());
        
        // Verificamos que el permiso 1 pasó a INACTIVO (Baja lógica)
        assertEquals(EstadoLogico.INACTIVO, rpExistente.getEstado());
        // Verificamos que ahora tiene 2 relaciones RolPermiso en total (el 1 inactivo, y el 2 activo recién creado)
        assertEquals(2, rolExistente.getRolPermisos().size()); 
    }

    @Test
    void actualizarRol_DebeLanzarExcepcion_SiIntentaPonerUnNombreQueYaExiste() {
        // Arrange
        RolUpdateDTO updateDTO = new RolUpdateDTO();
        updateDTO.setNombre("OTRO_ROL_EXISTENTE");

        when(rolRepository.findById(1)).thenReturn(Optional.of(rolExistente));
        when(rolRepository.existsByNombre("OTRO_ROL_EXISTENTE")).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            rolService.actualizarRol(1, updateDTO);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("El nombre del rol ya existe", exception.getReason());
    }
}
