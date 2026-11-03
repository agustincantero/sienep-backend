package com.fides.sienep.service;

import com.fides.sienep.dto.ArchivoDescargaDTO;
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
import com.fides.sienep.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

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

    @Mock
    private FileStorageService fileStorageService;

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

        when(usuarioRepository.existsByDocumentoAndPaisDocumento(anyString(), any())).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
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

        when(usuarioRepository.existsByDocumentoAndPaisDocumento(anyString(), any())).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
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
        rolAdmin.agregarPermiso(new Permiso("DESACTIVAR_ROL", "Permite eliminar roles"));

        // El actor solo tiene CREAR_FUNCIONARIO, no DESACTIVAR_ROL — no puede asignar rolAdmin
        mockAuthConPermisos("CREAR_FUNCIONARIO");

        when(usuarioRepository.existsByDocumentoAndPaisDocumento(anyString(), any())).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
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
        when(usuarioRepository.existsByDocumentoAndPaisDocumento(anyString(), any())).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.crear(requestDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear funcionario - Documento y país de un estudiante → 409 que nombra el documento")
    void crear_LanzaExcepcion_DocumentoDeEstudiante() {
        // Caso del bug: el chequeo era solo contra funcionarios, así que el choque con un
        // estudiante llegaba a la base y volvía como 409 genérico, que el front mostraba
        // como error de email. Ahora el chequeo es contra usuarios, con documento + país.
        requestDTO.setDocumento("41234604");
        requestDTO.setPaisDocumento(PaisDocumento.URY);
        when(usuarioRepository.existsByDocumentoAndPaisDocumento("41234604", PaisDocumento.URY)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.crear(requestDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        // El front elige bajo qué campo mostrar el error buscando "documento" en el mensaje.
        assertTrue(ex.getReason().toLowerCase().contains("documento"));
        assertFalse(ex.getReason().toLowerCase().contains("email"));
        verify(usuarioRepository, never()).existsByEmailIgnoreCase(anyString());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear funcionario - Email duplicado → 409")
    void crear_LanzaExcepcion_EmailDuplicado() {
        when(usuarioRepository.existsByDocumentoAndPaisDocumento(anyString(), any())).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.crear(requestDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear funcionario - Email existente con otras mayúsculas → 409 que nombra el email")
    void crear_LanzaExcepcion_EmailDuplicadoConOtrasMayusculas() {
        requestDTO.setEmail("Carlos.Perez@UTEC.edu.uy");
        when(usuarioRepository.existsByDocumentoAndPaisDocumento(anyString(), any())).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase("Carlos.Perez@UTEC.edu.uy")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.crear(requestDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().toLowerCase().contains("email"));
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear funcionario - CI uruguaya inválida → 400")
    void crear_LanzaExcepcion_CIInvalida() {
        // Cambiamos a URY con dígito verificador incorrecto
        requestDTO.setPaisDocumento(PaisDocumento.URY);
        requestDTO.setDocumento("47520642");
        when(usuarioRepository.existsByDocumentoAndPaisDocumento(anyString(), any())).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

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
        when(usuarioRepository.existsByDocumentoAndPaisDocumento(anyString(), any())).thenReturn(false);
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
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
    @DisplayName("Listar - Sin parámetro estado, devuelve solo ACTIVOS")
    void listar_SinEstado_DevuelveSoloActivos() {
        List<Funcionario> funcionarios = List.of(funcionarioMock);
        List<FuncionarioResponseDTO> dtos = List.of(responseDTO);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Funcionario> pagina = new PageImpl<>(funcionarios, pageable, 1);

        when(funcionarioRepository.findByEstado(EstadoLogico.ACTIVO, pageable)).thenReturn(pagina);
        when(funcionarioMapper.toDtoList(funcionarios)).thenReturn(dtos);

        Page<FuncionarioResponseDTO> result = funcionarioService.listar(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Listar - Filtro por texto usa buscarPorTextoYEstado")
    void listar_ConFiltroTexto_UsaBuscarPorTextoYEstado() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Funcionario> pagina = new PageImpl<>(List.of(funcionarioMock), pageable, 1);

        when(funcionarioRepository.buscarPorTextoYEstado("Carlos", EstadoLogico.ACTIVO, pageable))
                .thenReturn(pagina);
        when(funcionarioMapper.toDtoList(List.of(funcionarioMock))).thenReturn(List.of(responseDTO));

        Page<FuncionarioResponseDTO> result = funcionarioService.listar("Carlos", EstadoLogico.ACTIVO, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(funcionarioRepository, never()).findByEstado(any(), any());
    }

    // =========================================================
    // ACTUALIZAR
    // =========================================================

    @Test
    @DisplayName("Editar funcionario - Éxito")
    void editar_Exito() {
        mockAuthConPermisos(); // rolMock no tiene permisos asignados, cualquier actor pasa

        FuncionarioUpdateDTO updateDTO = new FuncionarioUpdateDTO();
        updateDTO.setNombre("Carlos");
        updateDTO.setApellido("González");
        // Mismo email que el mock → el chequeo excluye al propio funcionario (IdUsuarioNot)
        updateDTO.setEmail("carlos.perez@utec.edu.uy");
        updateDTO.setIdRol(1);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(rolRepository.findById(1)).thenReturn(Optional.of(rolMock));
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioMock);
        when(funcionarioMapper.toDto(any(Funcionario.class))).thenReturn(responseDTO);

        FuncionarioResponseDTO result = funcionarioService.editar(1, updateDTO);

        assertNotNull(result);
        assertEquals("González", funcionarioMock.getApellido());
        verify(funcionarioRepository).save(funcionarioMock);
    }

    @Test
    @DisplayName("Editar funcionario - Éxito cuando el actor ya tiene todos los permisos del rol destino")
    void editar_Exito_CuandoActorTieneLosPermisosDelRolDestino() {
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

        FuncionarioResponseDTO result = funcionarioService.editar(1, updateDTO);

        assertNotNull(result);
        verify(funcionarioRepository).save(funcionarioMock);
    }

    @Test
    @DisplayName("Editar funcionario - Rechaza asignar un rol con permisos que el actor no tiene (403)")
    void editar_LanzaForbidden_CuandoRolDestinoTienePermisoQueElActorNoTiene() {
        Rol rolAdmin = new Rol("ADMINISTRADOR");
        rolAdmin.setIdRol(2);
        rolAdmin.agregarPermiso(new Permiso("EDITAR_FUNCIONARIO", "Permite editar funcionarios"));
        rolAdmin.agregarPermiso(new Permiso("DESACTIVAR_ROL", "Permite eliminar roles"));

        // El actor solo tiene EDITAR_FUNCIONARIO, no DESACTIVAR_ROL — no puede asignar rolAdmin
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
                () -> funcionarioService.editar(1, updateDTO));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar funcionario - Rechaza tocar a un funcionario cuyo rol ACTUAL tiene permisos que el actor no tiene (403)")
    void editar_LanzaForbidden_CuandoFuncionarioActualTienePermisoQueElActorNoTiene() {
        // rolMock es el rol ACTUAL de funcionarioMock (la "víctima")
        rolMock.agregarPermiso(new Permiso("EDITAR_ESTUDIANTE", "Permite editar estudiantes"));

        // El actor tiene EDITAR_FUNCIONARIO y hasta los permisos del rol de DESTINO,
        // pero no EDITAR_ESTUDIANTE — que es un permiso que la víctima YA tenía antes
        // de esta edición. No alcanza con calificar para el rol nuevo si no calificás
        // también para el que la víctima ya tenía (evita degradar a alguien con más
        // privilegios reasignándolo a un rol que el actor sí puede otorgar).
        mockAuthConPermisos("EDITAR_FUNCIONARIO");

        FuncionarioUpdateDTO updateDTO = new FuncionarioUpdateDTO();
        updateDTO.setNombre("Carlos");
        updateDTO.setApellido("González");
        updateDTO.setEmail("carlos.perez@utec.edu.uy");
        updateDTO.setIdRol(1); // mismo rol, ni siquiera lo está cambiando

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(rolRepository.findById(1)).thenReturn(Optional.of(rolMock));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.editar(1, updateDTO));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar funcionario - No encontrado → 404")
    void editar_LanzaExcepcion_NoEncontrado() {
        FuncionarioUpdateDTO updateDTO = new FuncionarioUpdateDTO();
        updateDTO.setNombre("Carlos");
        updateDTO.setApellido("González");
        updateDTO.setEmail("otro@utec.edu.uy");
        updateDTO.setIdRol(1);

        when(funcionarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.editar(99, updateDTO));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar funcionario - Email de otro usuario (ej. un estudiante) → 409")
    void editar_LanzaExcepcion_EmailDeOtroUsuario() {
        FuncionarioUpdateDTO updateDTO = new FuncionarioUpdateDTO();
        updateDTO.setNombre("Carlos");
        updateDTO.setApellido("Pérez");
        updateDTO.setEmail("ana.garcia@utec.edu.uy");
        updateDTO.setIdRol(1);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(usuarioRepository.existsByEmailIgnoreCaseAndIdUsuarioNot("ana.garcia@utec.edu.uy", 1)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.editar(1, updateDTO));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().toLowerCase().contains("email"));
        verify(funcionarioRepository, never()).save(any());
    }

    // =========================================================
    // BAJA Y REACTIVACIÓN
    // =========================================================

    @Test
    @DisplayName("Desactivar - Estado pasa a INACTIVO")
    void desactivar_DebePonerEstadoInactivo() {
        mockAuthConPermisos(); // rolMock no tiene permisos asignados, cualquier actor pasa
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioMock);

        funcionarioService.desactivar(1);

        assertEquals(EstadoLogico.INACTIVO, funcionarioMock.getEstado());
        verify(funcionarioRepository).save(funcionarioMock);
        verify(funcionarioRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Reactivar funcionario - Estado pasa a ACTIVO")
    void reactivar_DebePonerEstadoActivo() {
        mockAuthConPermisos(); // rolMock no tiene permisos asignados, cualquier actor pasa
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

    @Test
    @DisplayName("Desactivar - Ya está inactivo (409)")
    void desactivar_LanzaConflict_YaInactivo() {
        mockAuthConPermisos(); // rolMock no tiene permisos asignados, cualquier actor pasa
        funcionarioMock.setEstado(EstadoLogico.INACTIVO);
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> funcionarioService.desactivar(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar funcionario - Ya está activo (409)")
    void reactivar_LanzaConflict_YaActivo() {
        mockAuthConPermisos(); // rolMock no tiene permisos asignados, cualquier actor pasa
        // El constructor "alta" de Funcionario arranca en PENDIENTE_DE_ACTIVACION, no ACTIVO
        funcionarioMock.setEstado(EstadoLogico.ACTIVO);
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> funcionarioService.reactivar(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desactivar - Rechaza tocar a un funcionario con un rol con permisos que el actor no tiene (403)")
    void desactivar_LanzaForbidden_CuandoFuncionarioTienePermisoQueElActorNoTiene() {
        rolMock.agregarPermiso(new Permiso("EDITAR_ESTUDIANTE", "Permite editar estudiantes"));

        // El actor solo tiene DESACTIVAR_FUNCIONARIO, no EDITAR_ESTUDIANTE (que sí tiene el
        // rol actual de la víctima) — no puede tocarla aunque tenga el permiso de la acción.
        mockAuthConPermisos("DESACTIVAR_FUNCIONARIO");
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> funcionarioService.desactivar(1));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar - Rechaza tocar a un funcionario con un rol con permisos que el actor no tiene (403)")
    void reactivar_LanzaForbidden_CuandoFuncionarioTienePermisoQueElActorNoTiene() {
        rolMock.agregarPermiso(new Permiso("EDITAR_ESTUDIANTE", "Permite editar estudiantes"));
        funcionarioMock.setEstado(EstadoLogico.INACTIVO);

        mockAuthConPermisos("REACTIVAR_FUNCIONARIO");
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> funcionarioService.reactivar(1));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
    }

    // =========================================================
    // RESTABLECER CONTRASEÑA
    // =========================================================

    @Test
    @DisplayName("Restablecer contraseña - Éxito, deja al funcionario PENDIENTE_DE_ACTIVACION")
    void restablecerContrasenia_Exito() {
        // Arranca ACTIVO (caso realista: un admin resetea la contraseña de un
        // funcionario que ya venía usando el sistema con normalidad).
        funcionarioMock.setEstado(EstadoLogico.ACTIVO);

        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(passwordGeneratorService.generar()).thenReturn("NuevaPassGenerada123!");
        when(passwordEncoder.encode("NuevaPassGenerada123!")).thenReturn("hashedNuevaPass");
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioMock);

        funcionarioService.restablecerContrasenia(1);

        assertEquals("hashedNuevaPass", funcionarioMock.getContrasenia());
        // El reset admin obliga al próximo login a elegir una contraseña propia
        // (JwtRequestFilter restringe a un usuario en este estado) — sin esto, un
        // funcionario ya ACTIVO seguiría ACTIVO después del reset y el enforcement
        // nunca se dispararía.
        assertEquals(EstadoLogico.PENDIENTE_DE_ACTIVACION, funcionarioMock.getEstado());
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

    @Test
    @DisplayName("Restablecer contraseña - Funcionario inactivo (409), no lo reactiva silenciosamente")
    void restablecerContrasenia_FuncionarioInactivo_LanzaConflict() {
        funcionarioMock.setEstado(EstadoLogico.INACTIVO);
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> funcionarioService.restablecerContrasenia(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(funcionarioRepository, never()).save(any());
        verify(emailService, never()).enviarContraseniaRestablecida(any(), any(), any());
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

    // =========================================================
    // FOTO DE PERFIL
    // =========================================================

    @Test
    @DisplayName("Subir foto - Éxito, guarda la ruta relativa devuelta por FileStorageService")
    void subirFoto_Exito() {
        MultipartFile foto = new MockMultipartFile("foto", "perfil.png", "image/png", "contenido".getBytes());
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(fileStorageService.guardar(eq(foto), eq("fotos/1"), anyList(), anyLong())).thenReturn("abc.png");
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(funcionarioMapper.toDto(funcionarioMock)).thenReturn(responseDTO);

        FuncionarioResponseDTO resultado = funcionarioService.subirFoto(1, foto);

        assertSame(responseDTO, resultado);
        assertEquals("fotos/1/abc.png", funcionarioMock.getFoto());
    }

    @Test
    @DisplayName("Subir foto - Si el save() falla, limpia el archivo físico y restaura la ruta anterior")
    void subirFoto_LimpiaArchivoFisico_SiFallaElSave() {
        funcionarioMock.setFoto("fotos/1/vieja.png");
        MultipartFile foto = new MockMultipartFile("foto", "perfil.png", "image/png", "contenido".getBytes());
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(fileStorageService.guardar(eq(foto), eq("fotos/1"), anyList(), anyLong())).thenReturn("abc.png");
        when(funcionarioRepository.save(any(Funcionario.class)))
                .thenThrow(new DataIntegrityViolationException("constraint violation"));

        assertThrows(DataIntegrityViolationException.class, () -> funcionarioService.subirFoto(1, foto));

        assertEquals("fotos/1/vieja.png", funcionarioMock.getFoto());
        verify(fileStorageService).eliminarSiExiste("fotos/1/abc.png");
    }

    @Test
    @DisplayName("Subir foto - Funcionario no encontrado (404), no guarda ningún archivo")
    void subirFoto_LanzaExcepcion_NoEncontrado() {
        MultipartFile foto = new MockMultipartFile("foto", "perfil.png", "image/png", "contenido".getBytes());
        when(funcionarioRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> funcionarioService.subirFoto(99, foto));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("Obtener foto - Éxito")
    void obtenerFoto_Exito() {
        funcionarioMock.setFoto("fotos/1/abc.png");
        Resource recurso = mock(Resource.class);
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));
        when(fileStorageService.cargar("fotos/1/abc.png")).thenReturn(recurso);
        when(fileStorageService.extraerExtension("fotos/1/abc.png")).thenReturn("png");
        when(fileStorageService.mediaTypeDeExtension("png")).thenReturn(MediaType.IMAGE_PNG);

        ArchivoDescargaDTO resultado = funcionarioService.obtenerFoto(1);

        assertSame(recurso, resultado.getRecurso());
        assertEquals(MediaType.IMAGE_PNG, resultado.getContentType());
        assertEquals("foto_1.png", resultado.getNombreDescarga());
    }

    @Test
    @DisplayName("Obtener foto - Sin foto cargada (404)")
    void obtenerFoto_SinFoto_404() {
        when(funcionarioRepository.findById(1)).thenReturn(Optional.of(funcionarioMock));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> funcionarioService.obtenerFoto(1));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(fileStorageService);
    }
}
