package com.fides.sienep.service;

import com.fides.sienep.dto.CambiarContraseniaDTO;
import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.dto.FuncionarioUpdateDTO;
import com.fides.sienep.mapper.FuncionarioMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.RolRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordGeneratorService passwordGeneratorService;

    @Mock
    private EmailService emailService;

    @Mock
    private FuncionarioMapper funcionarioMapper;

    @InjectMocks
    private FuncionarioService funcionarioService;

    private Rol rolMock;
    private Funcionario funcionarioMock;
    private FuncionarioRequestDTO requestDTO;
    private FuncionarioResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        rolMock = new Rol("Psicopedagogo");
        rolMock.setIdRol(1);

        // Usamos documento ARG para evitar que el validador de CI uruguaya
        // interfiera en tests que no están probando esa validación
        funcionarioMock = new Funcionario(
                "Carlos", "Pérez", "12345678",
                PaisDocumento.ARG, "hashedPassword",
                "carlos.perez@utec.edu.uy", rolMock
        );

        requestDTO = new FuncionarioRequestDTO();
        requestDTO.setNombre("Carlos");
        requestDTO.setApellido("Pérez");
        requestDTO.setDocumento("12345678");
        requestDTO.setPaisDocumento(PaisDocumento.ARG);
        requestDTO.setEmail("carlos.perez@utec.edu.uy");
        requestDTO.setIdRol(1);

        responseDTO = FuncionarioResponseDTO.builder()
                .idUsuario(1)
                .nombre("Carlos")
                .apellido("Pérez")
                .documento("12345678")
                .paisDocumento(PaisDocumento.ARG)
                .email("carlos.perez@utec.edu.uy")
                .estado(EstadoLogico.PENDIENTE_DE_ACTIVACION)
                .rol("Psicopedagogo")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthConPermisos(String... permisos) {
        Authentication auth = mock(Authentication.class);
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String permiso : permisos) {
            authorities.add(new SimpleGrantedAuthority(permiso));
        }
        doReturn(authorities).when(auth).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================================
    // CREAR
    // =========================================================

    @Test
    @DisplayName("Crear funcionario - Éxito")
    void crear_Exito() {
        mockAuthConPermisos(); // rolMock no tiene permisos asignados, cualquier actor pasa

        when(funcionarioRepository.existsByDocumento(anyString())).thenReturn(false);
        when(funcionarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(rolRepository.findById(1)).thenReturn(Optional.of(rolMock));
        when(passwordGeneratorService.generar()).thenReturn("RandomPass123!");
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioMock);
        when(funcionarioMapper.toDto(any(Funcionario.class))).thenReturn(responseDTO);

        FuncionarioResponseDTO result = funcionarioService.crear(requestDTO);

        assertNotNull(result);
        assertEquals("Carlos", result.getNombre());
        assertEquals("Psicopedagogo", result.getRol());
        assertNull(result.getAdvertencia());
        verify(funcionarioRepository).save(any(Funcionario.class));
    }

    @Test
    @DisplayName("Crear funcionario - Éxito aunque falle el envío del email (RF05/RF32 no debe ser bloqueante)")
    void crear_Exito_AunqueFalleEmail() {
        mockAuthConPermisos();

        when(funcionarioRepository.existsByDocumento(anyString())).thenReturn(false);
        when(funcionarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(rolRepository.findById(1)).thenReturn(Optional.of(rolMock));
        when(passwordGeneratorService.generar()).thenReturn("RandomPass123!");
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioMock);
        when(funcionarioMapper.toDto(any(Funcionario.class))).thenReturn(responseDTO);
        doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SMTP no disponible"))
                .when(emailService).enviarContraseniaTemporal(any(), any(), any());

        FuncionarioResponseDTO result = assertDoesNotThrow(() -> funcionarioService.crear(requestDTO));

        assertNotNull(result);
        assertNotNull(result.getAdvertencia());
        verify(funcionarioRepository).save(any(Funcionario.class));
    }

    @Test
    @DisplayName("Crear funcionario - Rechaza asignar un rol con permisos que el actor no tiene (403)")
    void crear_LanzaForbidden_CuandoRolDestinoTienePermisoQueElActorNoTiene() {
        Rol rolAdmin = new Rol("ADMINISTRADOR");
        rolAdmin.setIdRol(1);
        rolAdmin.agregarPermiso(new Permiso("CREAR_FUNCIONARIO", "Permite registrar un nuevo funcionario"));
        rolAdmin.agregarPermiso(new Permiso("ELIMINAR_ROL", "Permite eliminar roles"));

        // El actor solo tiene CREAR_FUNCIONARIO, no ELIMINAR_ROL — no puede asignar rolAdmin
        mockAuthConPermisos("CREAR_FUNCIONARIO");

        when(funcionarioRepository.existsByDocumento(anyString())).thenReturn(false);
        when(funcionarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(rolRepository.findById(1)).thenReturn(Optional.of(rolAdmin));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.crear(requestDTO));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear funcionario - Documento duplicado → 409")
    void crear_LanzaExcepcion_DocumentoDuplicado() {
        when(funcionarioRepository.existsByDocumento(anyString())).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.crear(requestDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear funcionario - Email duplicado → 409")
    void crear_LanzaExcepcion_EmailDuplicado() {
        when(funcionarioRepository.existsByDocumento(anyString())).thenReturn(false);
        when(funcionarioRepository.existsByEmail(anyString())).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.crear(requestDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear funcionario - CI uruguaya inválida → 400")
    void crear_LanzaExcepcion_CIInvalida() {
        // Cambiamos a URY con dígito verificador incorrecto
        requestDTO.setPaisDocumento(PaisDocumento.URY);
        requestDTO.setDocumento("47520642");
        when(funcionarioRepository.existsByDocumento(anyString())).thenReturn(false);
        when(funcionarioRepository.existsByEmail(anyString())).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.crear(requestDTO));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear funcionario - Rol no encontrado → 404")
    void crear_LanzaExcepcion_RolNoEncontrado() {
        // ARG para saltear validación de CI — este test prueba solo la búsqueda del rol
        when(funcionarioRepository.existsByDocumento(anyString())).thenReturn(false);
        when(funcionarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(rolRepository.findById(1)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.crear(requestDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    // =========================================================
    // OBTENER
    // =========================================================

    @Test
    @DisplayName("Obtener por ID - Éxito")
    void obtenerPorId_Exito() {
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(funcionarioMapper.toDto(funcionarioMock)).thenReturn(responseDTO);

        FuncionarioResponseDTO result = funcionarioService.obtenerPorId(1);

        assertNotNull(result);
        assertEquals("Carlos", result.getNombre());
        verify(funcionarioMapper).toDto(funcionarioMock);
    }

    @Test
    @DisplayName("Obtener por ID - No encontrado → 404")
    void obtenerPorId_LanzaExcepcion_NoEncontrado() {
        when(funcionarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.obtenerPorId(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(funcionarioMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Obtener todos - Éxito")
    void obtenerTodos_Exito() {
        List<Funcionario> funcionarios = List.of(funcionarioMock);
        List<FuncionarioResponseDTO> dtos = List.of(responseDTO);

        when(funcionarioRepository.findAll()).thenReturn(funcionarios);
        when(funcionarioMapper.toDtoList(funcionarios)).thenReturn(dtos);

        List<FuncionarioResponseDTO> result = funcionarioService.obtenerTodos();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // =========================================================
    // ACTUALIZAR
    // =========================================================

    @Test
    @DisplayName("Actualizar funcionario - Éxito")
    void actualizar_Exito() {
        mockAuthConPermisos(); // rolMock no tiene permisos asignados, cualquier actor pasa

        FuncionarioUpdateDTO updateDTO = new FuncionarioUpdateDTO();
        updateDTO.setNombre("Carlos");
        updateDTO.setApellido("González");
        // Mismo email que el mock → el service no llega a verificar duplicado
        updateDTO.setEmail("carlos.perez@utec.edu.uy");
        updateDTO.setIdRol(1);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(rolRepository.findById(1)).thenReturn(Optional.of(rolMock));
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioMock);
        when(funcionarioMapper.toDto(any(Funcionario.class))).thenReturn(responseDTO);

        FuncionarioResponseDTO result = funcionarioService.actualizar(1, updateDTO);

        assertNotNull(result);
        assertEquals("González", funcionarioMock.getApellido());
        verify(funcionarioRepository).save(funcionarioMock);
    }

    @Test
    @DisplayName("Actualizar funcionario - Éxito cuando el actor ya tiene todos los permisos del rol destino")
    void actualizar_Exito_CuandoActorTieneLosPermisosDelRolDestino() {
        Rol rolAdmin = new Rol("ADMINISTRADOR");
        rolAdmin.setIdRol(2);
        rolAdmin.agregarPermiso(new Permiso("EDITAR_FUNCIONARIO", "Permite editar funcionarios"));

        mockAuthConPermisos("EDITAR_FUNCIONARIO", "CREAR_FUNCIONARIO");

        FuncionarioUpdateDTO updateDTO = new FuncionarioUpdateDTO();
        updateDTO.setNombre("Carlos");
        updateDTO.setApellido("González");
        updateDTO.setEmail("carlos.perez@utec.edu.uy");
        updateDTO.setIdRol(2);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(rolRepository.findById(2)).thenReturn(Optional.of(rolAdmin));
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioMock);
        when(funcionarioMapper.toDto(any(Funcionario.class))).thenReturn(responseDTO);

        FuncionarioResponseDTO result = funcionarioService.actualizar(1, updateDTO);

        assertNotNull(result);
        verify(funcionarioRepository).save(funcionarioMock);
    }

    @Test
    @DisplayName("Actualizar funcionario - Rechaza asignar un rol con permisos que el actor no tiene (403)")
    void actualizar_LanzaForbidden_CuandoRolDestinoTienePermisoQueElActorNoTiene() {
        Rol rolAdmin = new Rol("ADMINISTRADOR");
        rolAdmin.setIdRol(2);
        rolAdmin.agregarPermiso(new Permiso("EDITAR_FUNCIONARIO", "Permite editar funcionarios"));
        rolAdmin.agregarPermiso(new Permiso("ELIMINAR_ROL", "Permite eliminar roles"));

        // El actor solo tiene EDITAR_FUNCIONARIO, no ELIMINAR_ROL — no puede asignar rolAdmin
        mockAuthConPermisos("EDITAR_FUNCIONARIO");

        FuncionarioUpdateDTO updateDTO = new FuncionarioUpdateDTO();
        updateDTO.setNombre("Carlos");
        updateDTO.setApellido("González");
        updateDTO.setEmail("carlos.perez@utec.edu.uy");
        updateDTO.setIdRol(2);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(rolRepository.findById(2)).thenReturn(Optional.of(rolAdmin));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.actualizar(1, updateDTO));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar funcionario - No encontrado → 404")
    void actualizar_LanzaExcepcion_NoEncontrado() {
        FuncionarioUpdateDTO updateDTO = new FuncionarioUpdateDTO();
        updateDTO.setNombre("Carlos");
        updateDTO.setApellido("González");
        updateDTO.setEmail("otro@utec.edu.uy");
        updateDTO.setIdRol(1);

        when(funcionarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.actualizar(99, updateDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    // =========================================================
    // BAJA Y REACTIVACIÓN
    // =========================================================

    @Test
    @DisplayName("Dar de baja - Estado pasa a INACTIVO")
    void darDeBaja_DebePonerEstadoInactivo() {
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioMock);

        funcionarioService.darDeBaja(1);

        assertEquals(EstadoLogico.INACTIVO, funcionarioMock.getEstado());
        verify(funcionarioRepository).save(funcionarioMock);
        verify(funcionarioRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Reactivar funcionario - Estado pasa a ACTIVO")
    void reactivar_DebePonerEstadoActivo() {
        funcionarioMock.setEstado(EstadoLogico.INACTIVO);
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioMock);
        when(funcionarioMapper.toDto(any(Funcionario.class))).thenReturn(responseDTO);

        FuncionarioResponseDTO result = funcionarioService.reactivar(1);

        assertNotNull(result);
        assertEquals(EstadoLogico.ACTIVO, funcionarioMock.getEstado());
        verify(funcionarioRepository).save(funcionarioMock);
        verify(funcionarioRepository, never()).deleteById(any());
    }

    // =========================================================
    // RESTABLECER CONTRASEÑA
    // =========================================================

    @Test
    @DisplayName("Restablecer contraseña - Éxito")
    void restablecerContrasenia_Exito() {
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(passwordGeneratorService.generar()).thenReturn("NuevaPassGenerada123!");
        when(passwordEncoder.encode("NuevaPassGenerada123!")).thenReturn("hashedNuevaPass");
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioMock);

        funcionarioService.restablecerContrasenia(1);

        assertEquals("hashedNuevaPass", funcionarioMock.getContrasenia());
        verify(funcionarioRepository).save(funcionarioMock);
        verify(emailService).enviarContraseniaRestablecida(
                funcionarioMock.getEmail(), funcionarioMock.getNombre(), "NuevaPassGenerada123!");
    }

    @Test
    @DisplayName("Restablecer contraseña - No encontrado (404)")
    void restablecerContrasenia_LanzaNotFound() {
        when(funcionarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.restablecerContrasenia(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    // =========================================================
    // CAMBIAR CONTRASEÑA
    // =========================================================

    @Test
    @DisplayName("Cambiar contraseña - Éxito")
    void cambiarContrasenia_Exito() {
        CambiarContraseniaDTO dto = new CambiarContraseniaDTO();
        dto.setContraseniaActual("ViejaContrasenia123!");
        dto.setContrasenia("NuevaContrasenia123!");

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(passwordEncoder.matches("ViejaContrasenia123!", funcionarioMock.getContrasenia())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedNueva");
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioMock);

        funcionarioService.cambiarContrasenia(1, dto);

        assertEquals("hashedNueva", funcionarioMock.getContrasenia());
        verify(funcionarioRepository).save(funcionarioMock);
    }

    @Test
    @DisplayName("Cambiar contraseña - Contraseña actual incorrecta (401)")
    void cambiarContrasenia_LanzaExcepcion_ContraseniaActualIncorrecta() {
        CambiarContraseniaDTO dto = new CambiarContraseniaDTO();
        dto.setContraseniaActual("ContraseniaIncorrecta");
        dto.setContrasenia("NuevaContrasenia123!");

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(passwordEncoder.matches("ContraseniaIncorrecta", funcionarioMock.getContrasenia())).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.cambiarContrasenia(1, dto));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cambiar contraseña - Estado PENDIENTE_DE_ACTIVACION pasa a ACTIVO")
    void cambiarContrasenia_ActivaEstadoPendiente() {
        funcionarioMock.setEstado(EstadoLogico.PENDIENTE_DE_ACTIVACION);

        CambiarContraseniaDTO dto = new CambiarContraseniaDTO();
        dto.setContraseniaActual("ViejaContrasenia123!");
        dto.setContrasenia("NuevaContrasenia123!");

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(passwordEncoder.matches("ViejaContrasenia123!", funcionarioMock.getContrasenia())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedNueva");
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioMock);

        funcionarioService.cambiarContrasenia(1, dto);

        assertEquals(EstadoLogico.ACTIVO, funcionarioMock.getEstado());
        verify(funcionarioRepository).save(funcionarioMock);
    }

    @Test
    @DisplayName("Obtener por documento - Éxito")
    void obtenerPorDocumento_Exito() {
        when(funcionarioRepository.findByDocumentoAndEstado("12345678", EstadoLogico.ACTIVO))
                .thenReturn(Optional.of(funcionarioMock));
        when(funcionarioMapper.toDto(funcionarioMock)).thenReturn(responseDTO);

        FuncionarioResponseDTO result = funcionarioService.obtenerPorDocumento("12345678");

        assertNotNull(result);
        assertEquals("12345678", result.getDocumento());
        verify(funcionarioMapper).toDto(funcionarioMock);
    }

    @Test
    @DisplayName("Obtener por documento - No encontrado → 404")
    void obtenerPorDocumento_LanzaExcepcion_NoEncontrado() {
        when(funcionarioRepository.findByDocumentoAndEstado("99999999", EstadoLogico.ACTIVO))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.obtenerPorDocumento("99999999"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(funcionarioMapper, never()).toDto(any());
    }
}