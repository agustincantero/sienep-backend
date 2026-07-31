package com.fides.sienep.service;

import com.fides.sienep.dto.ArchivoDescargaDTO;
import com.fides.sienep.dto.CambiarContraseniaDTO;
import com.fides.sienep.dto.EstudianteRequestDTO;
import com.fides.sienep.dto.EstudianteResponseDTO;
import com.fides.sienep.dto.EstudianteResumenDTO;
import com.fides.sienep.dto.EstudianteUpdateDTO;
import com.fides.sienep.dto.MisDatosRequestDTO;
import com.fides.sienep.mapper.EstudianteMapper;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Telefono;
import com.fides.sienep.model.Grupo;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.GrupoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordGeneratorService passwordGeneratorService;

    @Mock
    private EmailService emailService;

    @Mock
    private EstudianteMapper estudianteMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private EstudianteService estudianteService;

    private Estudiante estudianteExistente;
    private EstudianteResponseDTO responseDTO;
    private EstudianteResumenDTO resumenDTO;

    @BeforeEach
    void setUp() {
        estudianteExistente = new Estudiante();
        estudianteExistente.setNombre("Ana");
        estudianteExistente.setApellido("García");
        estudianteExistente.setDocumento("12345678");
        estudianteExistente.setPaisDocumento(PaisDocumento.ARG);
        estudianteExistente.setEmail("ana.garcia@utec.edu.uy");
        estudianteExistente.setFechaNacimiento(LocalDate.of(2000, 3, 15));
        estudianteExistente.setEstado(EstadoLogico.ACTIVO);
        estudianteExistente.setTelefonos(new ArrayList<>());
        estudianteExistente.setGrupos(new ArrayList<>());

        responseDTO = EstudianteResponseDTO.builder()
                .nombre("Ana")
                .apellido("García")
                .documento("12345678")
                .estado(EstadoLogico.ACTIVO)
                .telefonos(new ArrayList<>())
                .grupos(new ArrayList<>())
                .build();

        resumenDTO = EstudianteResumenDTO.builder()
                .nombre("Ana")
                .apellido("García")
                .documento("12345678")
                .estado(EstadoLogico.ACTIVO)
                .grupos(new ArrayList<>())
                .telefonos(new ArrayList<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Helpers de seguridad ──────────────────────────────────

    private Authentication mockAuth(String idUsuario, boolean esEstudiante) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(idUsuario);
        if (esEstudiante) {
            List<org.springframework.security.core.GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(() -> "ROLE_ESTUDIANTE");
            lenient().doReturn(authorities).when(auth).getAuthorities();
        } else {
            lenient().doReturn(new ArrayList<>()).when(auth).getAuthorities();
        }
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    private Authentication mockAuthConPermiso(String idUsuario, String permiso) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(idUsuario);
        List<org.springframework.security.core.GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(() -> permiso);
        lenient().doReturn(authorities).when(auth).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    private Authentication mockAuthConPermisos(String idUsuario, String... permisos) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(idUsuario);
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String permiso : permisos) {
            authorities.add(new SimpleGrantedAuthority(permiso));
        }
        lenient().doReturn(authorities).when(auth).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    // =========================================================
    // OBTENER POR ID
    // =========================================================

    @Test
    @DisplayName("Obtener por ID - Éxito")
    void obtenerPorId_DebeRetornarDTO_CuandoElEstudianteExiste() {
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteMapper.toDto(estudianteExistente)).thenReturn(responseDTO);

        EstudianteResponseDTO resultado = estudianteService.obtenerPorId(1);

        assertNotNull(resultado);
        assertEquals("Ana", resultado.getNombre());
        verify(estudianteRepository).findById(1);
        verify(estudianteMapper).toDto(estudianteExistente);
    }

    @Test
    @DisplayName("Obtener por ID - No existe → 404")
    void obtenerPorId_DebeLanzar404_CuandoNoExiste() {
        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.obtenerPorId(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(estudianteMapper, never()).toDto(any());
    }

    // =========================================================
    // OBTENER RESUMEN POR ID
    // =========================================================

    @Test
    @DisplayName("Obtener resumen por ID - Éxito")
    void obtenerResumenPorId_Exito() {
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);

        EstudianteResumenDTO resultado = estudianteService.obtenerResumenPorId(1);

        assertNotNull(resultado);
        assertEquals("Ana", resultado.getNombre());
        verify(estudianteMapper).toResumenDto(estudianteExistente);
    }

    @Test
    @DisplayName("Obtener resumen por ID - No existe → 404")
    void obtenerResumenPorId_LanzaExcepcion_NoEncontrado() {
        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.obtenerResumenPorId(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(estudianteMapper, never()).toResumenDto(any());
    }

    // =========================================================
    // LISTAR
    // =========================================================

    @Test
    @DisplayName("Listar - Sin filtros devuelve página de activos")
    void listar_SinFiltros_DevuelvePaginaDeActivos() {
        mockAuthConPermiso("1", "VER_ESTUDIANTE");

        Page<Estudiante> pagina = new PageImpl<>(List.of(estudianteExistente));
        when(estudianteRepository.findByEstado(EstadoLogico.ACTIVO, PageRequest.of(0, 10)))
                .thenReturn(pagina);
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);

        Page<EstudianteResumenDTO> resultado = estudianteService.listar(
                null, null, null, null, EstadoLogico.ACTIVO, PageRequest.of(0, 10));

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Listar - Con filtro de texto sin BUSCAR_ESTUDIANTE lanza 403 (B5)")
    void listar_ConFiltroTexto_SinBuscarEstudiante_LanzaForbidden() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> estudianteService.listar("Ana", null, null, null, null, PageRequest.of(0, 10)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(estudianteRepository);
    }

    @Test
    @DisplayName("Listar - Sin filtros no exige BUSCAR_ESTUDIANTE (B5)")
    void listar_SinFiltros_NoExigeBuscarEstudiante() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO");

        Page<Estudiante> pagina = new PageImpl<>(List.of(estudianteExistente));
        when(estudianteRepository.findByEstado(EstadoLogico.ACTIVO, PageRequest.of(0, 10)))
                .thenReturn(pagina);
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);

        Page<EstudianteResumenDTO> resultado = estudianteService.listar(
                null, null, null, null, null, PageRequest.of(0, 10));

        assertNotNull(resultado);
    }

    @Test
    @DisplayName("Listar - Sin VER_ESTUDIANTE fuerza estado ACTIVO aunque se pida otro (B5)")
    void listar_SinVerEstudiante_FuerzaEstadoActivo() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO");

        Page<Estudiante> pagina = new PageImpl<>(List.of(estudianteExistente));
        when(estudianteRepository.findByEstado(EstadoLogico.ACTIVO, PageRequest.of(0, 10)))
                .thenReturn(pagina);
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);

        // Pide INACTIVO explícitamente, pero sin VER_ESTUDIANTE el service lo ignora
        estudianteService.listar(null, null, null, null, EstadoLogico.INACTIVO, PageRequest.of(0, 10));

        verify(estudianteRepository).findByEstado(EstadoLogico.ACTIVO, PageRequest.of(0, 10));
        verify(estudianteRepository, never()).findByEstado(eq(EstadoLogico.INACTIVO), any());
    }

    @Test
    @DisplayName("Listar - Con VER_ESTUDIANTE y estado null lista todos los estados (B5)")
    void listar_ConVerEstudiante_EstadoNull_ListaTodos() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO", "VER_ESTUDIANTE");

        Page<Estudiante> pagina = new PageImpl<>(List.of(estudianteExistente));
        when(estudianteRepository.findAll(PageRequest.of(0, 10))).thenReturn(pagina);
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);

        estudianteService.listar(null, null, null, null, null, PageRequest.of(0, 10));

        verify(estudianteRepository).findAll(PageRequest.of(0, 10));
        verify(estudianteRepository, never()).findByEstado(any(), any());
    }

    @Test
    @DisplayName("Listar - Con VER_ESTUDIANTE respeta el estado pedido (B5)")
    void listar_ConVerEstudiante_RespetaEstadoPedido() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO", "VER_ESTUDIANTE");

        Page<Estudiante> pagina = new PageImpl<>(List.of(estudianteExistente));
        when(estudianteRepository.findByEstado(EstadoLogico.INACTIVO, PageRequest.of(0, 10)))
                .thenReturn(pagina);
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);

        estudianteService.listar(null, null, null, null, EstadoLogico.INACTIVO, PageRequest.of(0, 10));

        verify(estudianteRepository).findByEstado(EstadoLogico.INACTIVO, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Listar - Filtro por texto con BUSCAR_ESTUDIANTE usa buscarPorNombreYEstado (B5)")
    void listar_ConFiltroTexto_UsaBuscarPorNombreYEstado() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO", "BUSCAR_ESTUDIANTE");

        Page<Estudiante> pagina = new PageImpl<>(List.of(estudianteExistente));
        when(estudianteRepository.buscarPorNombreYEstado("Ana", EstadoLogico.ACTIVO, PageRequest.of(0, 10)))
                .thenReturn(pagina);
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);

        estudianteService.listar("Ana", null, null, null, EstadoLogico.ACTIVO, PageRequest.of(0, 10));

        verify(estudianteRepository).buscarPorNombreYEstado("Ana", EstadoLogico.ACTIVO, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Listar - Filtro por grupo con BUSCAR_ESTUDIANTE usa findByGrupoAndEstado (B5)")
    void listar_ConFiltroGrupo_UsaFindByGrupoAndEstado() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO", "BUSCAR_ESTUDIANTE");

        Page<Estudiante> pagina = new PageImpl<>(List.of(estudianteExistente));
        when(estudianteRepository.findByGrupoAndEstado(1, EstadoLogico.ACTIVO, PageRequest.of(0, 10)))
                .thenReturn(pagina);
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);

        estudianteService.listar(null, 1, null, null, EstadoLogico.ACTIVO, PageRequest.of(0, 10));

        verify(estudianteRepository).findByGrupoAndEstado(1, EstadoLogico.ACTIVO, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Listar - Filtro por carrera con BUSCAR_ESTUDIANTE usa findByCarreraAndEstado (B5)")
    void listar_ConFiltroCarrera_UsaFindByCarreraAndEstado() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO", "BUSCAR_ESTUDIANTE");

        Page<Estudiante> pagina = new PageImpl<>(List.of(estudianteExistente));
        when(estudianteRepository.findByCarreraAndEstado(1, EstadoLogico.ACTIVO, PageRequest.of(0, 10)))
                .thenReturn(pagina);
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);

        estudianteService.listar(null, null, 1, null, EstadoLogico.ACTIVO, PageRequest.of(0, 10));

        verify(estudianteRepository).findByCarreraAndEstado(1, EstadoLogico.ACTIVO, PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Listar - Filtro por ITR con BUSCAR_ESTUDIANTE usa findByItrAndEstado (B5)")
    void listar_ConFiltroItr_UsaFindByItrAndEstado() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO", "BUSCAR_ESTUDIANTE");

        Page<Estudiante> pagina = new PageImpl<>(List.of(estudianteExistente));
        when(estudianteRepository.findByItrAndEstado(1, EstadoLogico.ACTIVO, PageRequest.of(0, 10)))
                .thenReturn(pagina);
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);

        estudianteService.listar(null, null, null, 1, EstadoLogico.ACTIVO, PageRequest.of(0, 10));

        verify(estudianteRepository).findByItrAndEstado(1, EstadoLogico.ACTIVO, PageRequest.of(0, 10));
    }

    // =========================================================
    // CREAR
    // =========================================================

    @Test
    @DisplayName("Crear - Éxito")
    void crear_Exito() {
        EstudianteRequestDTO dto = buildDtoValido();
        when(estudianteRepository.existsByDocumento(anyString())).thenReturn(false);
        when(estudianteRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordGeneratorService.generar()).thenReturn("RandomPass123!");
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudianteExistente);
        when(estudianteMapper.toDto(any(Estudiante.class))).thenReturn(responseDTO);

        EstudianteResponseDTO resultado = estudianteService.crear(dto);

        assertNotNull(resultado);
        assertEquals("Ana", resultado.getNombre());
        assertNull(resultado.getAdvertencia());
        verify(estudianteRepository).save(any(Estudiante.class));
    }

    @Test
    @DisplayName("Crear - Éxito aunque falle el envío del email (RF05 no debe ser bloqueante)")
    void crear_Exito_AunqueFalleEmail() {
        EstudianteRequestDTO dto = buildDtoValido();
        when(estudianteRepository.existsByDocumento(anyString())).thenReturn(false);
        when(estudianteRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordGeneratorService.generar()).thenReturn("RandomPass123!");
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudianteExistente);
        when(estudianteMapper.toDto(any(Estudiante.class))).thenReturn(responseDTO);
        doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "SMTP no disponible"))
                .when(emailService).enviarContraseniaTemporal(any(), any(), any());

        EstudianteResponseDTO resultado = assertDoesNotThrow(() -> estudianteService.crear(dto));

        assertNotNull(resultado);
        assertNotNull(resultado.getAdvertencia());
        verify(estudianteRepository).save(any(Estudiante.class));
    }

    @Test
    @DisplayName("Crear - Documento duplicado → 409")
    void crear_DebeLanzar409_CuandoDocumentoYaExiste() {
        EstudianteRequestDTO dto = buildDtoValido();
        when(estudianteRepository.existsByDocumento("12345678")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.crear(dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear - Email duplicado → 409")
    void crear_LanzaExcepcion_EmailDuplicado() {
        EstudianteRequestDTO dto = buildDtoValido();
        when(estudianteRepository.existsByDocumento(anyString())).thenReturn(false);
        when(estudianteRepository.existsByEmail(anyString())).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.crear(dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear - CI uruguaya inválida → 400")
    void crear_DebeLanzar400_CuandoCIUruguayaEsInvalida() {
        EstudianteRequestDTO dto = new EstudianteRequestDTO();
        dto.setNombre("Ana");
        dto.setApellido("García");
        dto.setDocumento("47520642");
        dto.setPaisDocumento(PaisDocumento.URY);
        dto.setEmail("ana.garcia@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(1998, 7, 20));

        when(estudianteRepository.existsByDocumento("47520642")).thenReturn(false);
        when(estudianteRepository.existsByEmail(dto.getEmail())).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.crear(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear - Menor de edad → 400")
    void crear_DebeLanzar400_CuandoEstudianteEsMenorDeEdad() {
        EstudianteRequestDTO dto = buildDtoValido();
        dto.setFechaNacimiento(LocalDate.now().minusYears(17));
        when(estudianteRepository.existsByDocumento(anyString())).thenReturn(false);
        when(estudianteRepository.existsByEmail(anyString())).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.crear(dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    // =========================================================
    // ACTUALIZAR
    // =========================================================

    @Test
    @DisplayName("Actualizar - Éxito")
    void actualizar_Exito() {
        EstudianteUpdateDTO dto = new EstudianteUpdateDTO();
        dto.setNombre("Ana");
        dto.setApellido("González");
        dto.setEmail("ana.garcia@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(2000, 3, 15));

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudianteExistente);
        when(estudianteMapper.toDto(any(Estudiante.class))).thenReturn(responseDTO);

        EstudianteResponseDTO resultado = estudianteService.actualizar(1, dto);

        assertNotNull(resultado);
        verify(estudianteRepository).save(estudianteExistente);
    }

    @Test
    @DisplayName("Actualizar - Da de baja lógica los teléfonos que ya no vienen en la lista (M1)")
    void actualizar_DaDeBajaTelefonosRemovidos() {
        Telefono telefonoViejo = new Telefono(estudianteExistente, "099111111");
        telefonoViejo.setEstado(EstadoLogico.ACTIVO);
        estudianteExistente.setTelefonos(new ArrayList<>(List.of(telefonoViejo)));

        EstudianteUpdateDTO dto = new EstudianteUpdateDTO();
        dto.setNombre("Ana");
        dto.setApellido("González");
        dto.setEmail("ana.garcia@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(2000, 3, 15));
        dto.setTelefonos(List.of("099222222"));

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudianteExistente);
        when(estudianteMapper.toDto(any(Estudiante.class))).thenReturn(responseDTO);

        estudianteService.actualizar(1, dto);

        assertEquals(EstadoLogico.ELIMINADO, telefonoViejo.getEstado());
        boolean nuevoTelefonoActivo = false;
        for (Telefono t : estudianteExistente.getTelefonos()) {
            if (t.getId().getNumero().equals("099222222") && t.getEstado() == EstadoLogico.ACTIVO) {
                nuevoTelefonoActivo = true;
            }
        }
        assertTrue(nuevoTelefonoActivo);
    }

    @Test
    @DisplayName("Actualizar - Reactiva un teléfono previamente eliminado que vuelve a la lista (M1)")
    void actualizar_ReactivaTelefonoEliminado() {
        Telefono telefonoEliminado = new Telefono(estudianteExistente, "099111111");
        telefonoEliminado.setEstado(EstadoLogico.ELIMINADO);
        estudianteExistente.setTelefonos(new ArrayList<>(List.of(telefonoEliminado)));

        EstudianteUpdateDTO dto = new EstudianteUpdateDTO();
        dto.setNombre("Ana");
        dto.setApellido("González");
        dto.setEmail("ana.garcia@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(2000, 3, 15));
        dto.setTelefonos(List.of("099111111"));

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudianteExistente);
        when(estudianteMapper.toDto(any(Estudiante.class))).thenReturn(responseDTO);

        estudianteService.actualizar(1, dto);

        assertEquals(EstadoLogico.ACTIVO, telefonoEliminado.getEstado());
        assertEquals(1, estudianteExistente.getTelefonos().size());
    }

    @Test
    @DisplayName("Actualizar - Rechaza asignar un grupo que no está ACTIVO (M2)")
    void actualizar_LanzaExcepcion_GrupoNoActivo() {
        Grupo grupoInactivo = new Grupo();
        grupoInactivo.setIdGrupo(1);
        grupoInactivo.setEstado(EstadoLogico.INACTIVO);

        EstudianteUpdateDTO dto = new EstudianteUpdateDTO();
        dto.setNombre("Ana");
        dto.setApellido("González");
        dto.setEmail("ana.garcia@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(2000, 3, 15));
        dto.setIdGrupos(List.of(1));

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(grupoRepository.findById(1)).thenReturn(Optional.of(grupoInactivo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> estudianteService.actualizar(1, dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar - No encontrado → 404")
    void actualizar_LanzaExcepcion_NoEncontrado() {
        EstudianteUpdateDTO dto = new EstudianteUpdateDTO();
        dto.setNombre("Ana");
        dto.setApellido("González");
        dto.setEmail("ana.garcia@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(2000, 3, 15));

        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.actualizar(99, dto));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    // =========================================================
    // EDITAR MIS DATOS
    // =========================================================

    @Test
    @DisplayName("Editar mis datos - Éxito")
    void editarMisDatos_Exito() {
        mockAuth("1", true);

        MisDatosRequestDTO dto = new MisDatosRequestDTO();
        dto.setCiudad("Montevideo");

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudianteExistente);
        when(estudianteMapper.toDto(any(Estudiante.class))).thenReturn(responseDTO);

        EstudianteResponseDTO resultado = estudianteService.editarMisDatos(1, dto);

        assertNotNull(resultado);
        verify(estudianteRepository).save(estudianteExistente);
    }

    @Test
    @DisplayName("Editar mis datos - Intenta editar otro estudiante → 403")
    void editarMisDatos_LanzaExcepcion_OtroEstudiante() {
        mockAuth("2", true);

        MisDatosRequestDTO dto = new MisDatosRequestDTO();
        dto.setCiudad("Montevideo");

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.editarMisDatos(1, dto));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    // =========================================================
    // VER MI PERFIL
    // =========================================================

    @Test
    @DisplayName("Ver mi perfil - Éxito")
    void verMiPerfil_Exito() {
        mockAuth("1", true);

        lenient().when(estudianteRepository.findById(anyInt()))
                .thenReturn(Optional.of(estudianteExistente));
        lenient().when(estudianteMapper.toResumenDto(estudianteExistente))
                .thenReturn(resumenDTO);

        EstudianteResumenDTO resultado = estudianteService.verMiPerfil();

        assertNotNull(resultado);
        assertEquals("Ana", resultado.getNombre());
        verify(estudianteMapper).toResumenDto(estudianteExistente);
    }

    // =========================================================
    // CAMBIAR CONTRASEÑA
    // =========================================================

    @Test
    @DisplayName("Cambiar contraseña - Éxito")
    void cambiarContrasenia_Exito() {
        mockAuth("1", true);
        estudianteExistente.setContrasenia("hashedContraseniaVieja");

        CambiarContraseniaDTO dto = new CambiarContraseniaDTO();
        dto.setContraseniaActual("ViejaContrasenia123!");
        dto.setContrasenia("NuevaContrasenia123!");

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(passwordEncoder.matches("ViejaContrasenia123!", "hashedContraseniaVieja")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedNueva");
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudianteExistente);

        estudianteService.cambiarContrasenia(1, dto);

        assertEquals("hashedNueva", estudianteExistente.getContrasenia());
        verify(estudianteRepository).save(estudianteExistente);
    }

    @Test
    @DisplayName("Cambiar contraseña - Contraseña actual incorrecta (401)")
    void cambiarContrasenia_LanzaExcepcion_ContraseniaActualIncorrecta() {
        mockAuth("1", true);
        estudianteExistente.setContrasenia("hashedContraseniaVieja");

        CambiarContraseniaDTO dto = new CambiarContraseniaDTO();
        dto.setContraseniaActual("ContraseniaIncorrecta");
        dto.setContrasenia("NuevaContrasenia123!");

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(passwordEncoder.matches("ContraseniaIncorrecta", "hashedContraseniaVieja")).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.cambiarContrasenia(1, dto));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @Disabled("Requiere contexto de Spring Security para validar @PreAuthorize")
    @DisplayName("Cambiar contraseña - Intenta cambiar la de otro → 403")
    void cambiarContrasenia_LanzaExcepcion_OtroUsuario() {
        mockAuth("2", true);

        CambiarContraseniaDTO dto = new CambiarContraseniaDTO();
        dto.setContraseniaActual("ViejaContrasenia123!");
        dto.setContrasenia("NuevaContrasenia123!");

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.cambiarContrasenia(1, dto));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cambiar contraseña - Estado PENDIENTE_DE_ACTIVACION pasa a ACTIVO")
    void cambiarContrasenia_ActivaEstadoPendiente() {
        mockAuth("1", true);

        estudianteExistente.setEstado(EstadoLogico.PENDIENTE_DE_ACTIVACION);
        estudianteExistente.setContrasenia("hashedContraseniaVieja");

        CambiarContraseniaDTO dto = new CambiarContraseniaDTO();
        dto.setContraseniaActual("ViejaContrasenia123!");
        dto.setContrasenia("NuevaContrasenia123!");

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(passwordEncoder.matches("ViejaContrasenia123!", "hashedContraseniaVieja")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedNueva");
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudianteExistente);

        estudianteService.cambiarContrasenia(1, dto);

        assertEquals(EstadoLogico.ACTIVO, estudianteExistente.getEstado());
        verify(estudianteRepository).save(estudianteExistente);
    }

    // =========================================================
    // RESTABLECER CONTRASEÑA
    // =========================================================

    @Test
    @DisplayName("Restablecer contraseña - Éxito")
    void restablecerContrasenia_Exito() {
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(passwordGeneratorService.generar()).thenReturn("NuevaPassGenerada123!");
        when(passwordEncoder.encode("NuevaPassGenerada123!")).thenReturn("hashedNuevaPass");
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudianteExistente);

        estudianteService.restablecerContrasenia(1);

        assertEquals("hashedNuevaPass", estudianteExistente.getContrasenia());
        verify(estudianteRepository).save(estudianteExistente);
        verify(emailService).enviarContraseniaRestablecida(
                estudianteExistente.getEmail(), estudianteExistente.getNombre(), "NuevaPassGenerada123!");
    }

    @Test
    @DisplayName("Restablecer contraseña - No encontrado (404)")
    void restablecerContrasenia_LanzaNotFound() {
        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.restablecerContrasenia(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    // =========================================================
    // FOTO DE PERFIL
    // =========================================================

    @Test
    @DisplayName("Subir foto - Éxito, guarda la ruta relativa devuelta por FileStorageService")
    void subirFoto_Exito() {
        MultipartFile foto = new MockMultipartFile(
                "foto", "perfil.jpg", "image/jpeg", "contenido".getBytes());

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(fileStorageService.guardar(eq(foto), eq("fotos/1"), anyList(), anyLong()))
                .thenReturn("3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.jpg");
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estudianteMapper.toDto(estudianteExistente)).thenReturn(responseDTO);

        EstudianteResponseDTO resultado = estudianteService.subirFoto(1, foto);

        assertNotNull(resultado);
        assertEquals("fotos/1/3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.jpg", estudianteExistente.getFoto());
        verify(estudianteRepository).save(estudianteExistente);
    }

    @Test
    @DisplayName("Subir foto - Estudiante no encontrado (404)")
    void subirFoto_LanzaExcepcion_NoEncontrado() {
        MultipartFile foto = new MockMultipartFile(
                "foto", "perfil.jpg", "image/jpeg", "contenido".getBytes());

        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> estudianteService.subirFoto(99, foto));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(fileStorageService);
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Obtener foto - Éxito")
    void obtenerFoto_Exito() {
        estudianteExistente.setFoto("fotos/1/3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.jpg");
        Resource recursoMock = mock(Resource.class);

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(fileStorageService.cargar("fotos/1/3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.jpg")).thenReturn(recursoMock);
        when(fileStorageService.extraerExtension(estudianteExistente.getFoto())).thenReturn("jpg");
        when(fileStorageService.mediaTypeDeExtension("jpg")).thenReturn(MediaType.IMAGE_JPEG);

        ArchivoDescargaDTO resultado = estudianteService.obtenerFoto(1);

        assertNotNull(resultado);
        assertEquals("foto_1.jpg", resultado.nombreDescarga());
        assertEquals(MediaType.IMAGE_JPEG, resultado.contentType());
        assertSame(recursoMock, resultado.recurso());
    }

    @Test
    @DisplayName("Obtener foto - Estudiante sin foto cargada (404)")
    void obtenerFoto_LanzaExcepcion_SinFotoCargada() {
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> estudianteService.obtenerFoto(1));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("Obtener foto - Estudiante no encontrado (404)")
    void obtenerFoto_LanzaExcepcion_NoEncontrado() {
        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> estudianteService.obtenerFoto(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(fileStorageService);
    }

    // =========================================================
    // BAJA Y REACTIVACIÓN
    // =========================================================

    @Test
    @DisplayName("Dar de baja - Estado pasa a INACTIVO")
    void darDeBaja_DebePonerEstadoInactivo_YGuardar() {
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteRepository.save(any(Estudiante.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        estudianteService.darDeBaja(1);

        assertEquals(EstadoLogico.INACTIVO, estudianteExistente.getEstado());
        verify(estudianteRepository).save(estudianteExistente);
        verify(estudianteRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Reactivar - Estado pasa a ACTIVO")
    void reactivar_DebePonerEstadoActivo() {
        estudianteExistente.setEstado(EstadoLogico.INACTIVO);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudianteExistente);

        estudianteService.reactivar(1);

        assertEquals(EstadoLogico.ACTIVO, estudianteExistente.getEstado());
        verify(estudianteRepository).save(estudianteExistente);
        verify(estudianteRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Reactivar - No encontrado → 404")
    void reactivar_LanzaExcepcion_NoEncontrado() {
        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.reactivar(99));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    // =========================================================
    // HELPER
    // =========================================================

    private EstudianteRequestDTO buildDtoValido() {
        EstudianteRequestDTO dto = new EstudianteRequestDTO();
        dto.setNombre("Ana");
        dto.setApellido("García");
        dto.setDocumento("12345678");
        dto.setPaisDocumento(PaisDocumento.ARG);
        dto.setEmail("ana.garcia@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(1998, 7, 20));
        return dto;
    }
}