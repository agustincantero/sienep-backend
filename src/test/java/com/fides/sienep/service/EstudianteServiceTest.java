package com.fides.sienep.service;

import com.fides.sienep.dto.ArchivoDescargaDTO;
import com.fides.sienep.dto.CambiarContraseniaDTO;
import com.fides.sienep.dto.EstudianteRequestDTO;
import com.fides.sienep.dto.EstudianteResponseDTO;
import com.fides.sienep.dto.EstudianteResumenDTO;
import com.fides.sienep.dto.EstudianteUpdateDTO;
import com.fides.sienep.dto.MiPerfilEstudianteDTO;
import com.fides.sienep.dto.MisDatosRequestDTO;
import com.fides.sienep.mapper.EstudianteMapper;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Rol;
import com.fides.sienep.model.Telefono;
import com.fides.sienep.model.Grupo;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.GrupoRepository;
import com.fides.sienep.repository.RolRepository;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.dao.DataIntegrityViolationException;
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
    private RolRepository rolRepository;

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
                .informacionSalud("Diagnóstico reservado")
                .sistemaSalud("ASSE")
                .motivoDerivacion("Derivación por departamento académico")
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
    @DisplayName("Obtener por ID - Éxito, con VER_BLOQUE_CONFIDENCIAL muestra datos de salud")
    void obtenerPorId_DebeRetornarDTO_CuandoElEstudianteExiste() {
        mockAuthConPermisos("2", "VER_ESTUDIANTE", "VER_BLOQUE_CONFIDENCIAL");
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteMapper.toDto(estudianteExistente)).thenReturn(responseDTO);

        EstudianteResponseDTO resultado = estudianteService.obtenerPorId(1);

        assertNotNull(resultado);
        assertEquals("Ana", resultado.getNombre());
        assertEquals("Diagnóstico reservado", resultado.getInformacionSalud());
        assertEquals("ASSE", resultado.getSistemaSalud());
        assertEquals("Derivación por departamento académico", resultado.getMotivoDerivacion());
        verify(estudianteRepository).findById(1);
        verify(estudianteMapper).toDto(estudianteExistente);
    }

    @Test
    @DisplayName("Obtener por ID - Sin VER_BLOQUE_CONFIDENCIAL oculta datos de salud")
    void obtenerPorId_SinVerBloqueConfidencial_OcultaDatosDeSalud() {
        mockAuthConPermisos("2", "VER_ESTUDIANTE");
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteMapper.toDto(estudianteExistente)).thenReturn(responseDTO);

        EstudianteResponseDTO resultado = estudianteService.obtenerPorId(1);

        assertNotNull(resultado);
        assertEquals("Ana", resultado.getNombre());
        assertNull(resultado.getInformacionSalud());
        assertNull(resultado.getSistemaSalud());
        assertNull(resultado.getMotivoDerivacion());
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
        mockAuthConPermiso("1", "VER_ESTUDIANTE_RESUMIDO");
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

    @Test
    @DisplayName("Obtener resumen por ID - Con VER_ESTUDIANTE conserva la urlFoto")
    void obtenerResumenPorId_ConVerEstudiante_ConservaUrlFoto() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO", "VER_ESTUDIANTE");
        resumenDTO.setUrlFoto("/estudiantes/1/foto");
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);

        EstudianteResumenDTO resultado = estudianteService.obtenerResumenPorId(1);

        assertEquals("/estudiantes/1/foto", resultado.getUrlFoto());
    }

    @Test
    @DisplayName("Obtener resumen por ID - Sin VER_ESTUDIANTE oculta la urlFoto (el endpoint de foto daría 403)")
    void obtenerResumenPorId_SinVerEstudiante_OcultaUrlFoto() {
        mockAuthConPermiso("1", "VER_ESTUDIANTE_RESUMIDO");
        resumenDTO.setUrlFoto("/estudiantes/1/foto");
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);

        EstudianteResumenDTO resultado = estudianteService.obtenerResumenPorId(1);

        assertNull(resultado.getUrlFoto());
    }

    // =========================================================
    // LISTAR
    // =========================================================

    // Qué filtra cada combinación (texto + grupo + carrera + ITR + estado) se verifica contra Postgres real en
    // EstudianteBusquedaIT: con Mockito no se puede inspeccionar el contenido de una Specification. Acá se cubren
    // los permisos, la foto y que siempre sea una única consulta.
    private void stubBusqueda() {
        when(estudianteRepository.findAll(anySpec(), eq(PageRequest.of(0, 10))))
                .thenReturn(new PageImpl<>(List.of(estudianteExistente)));
        when(estudianteMapper.toResumenDto(estudianteExistente)).thenReturn(resumenDTO);
    }

    @SuppressWarnings("unchecked")
    private static Specification<Estudiante> anySpec() {
        return any(Specification.class);
    }

    @Test
    @DisplayName("Listar - Sin filtros devuelve la página de la consulta")
    void listar_SinFiltros_DevuelvePagina() {
        mockAuthConPermiso("1", "VER_ESTUDIANTE");
        stubBusqueda();

        Page<EstudianteResumenDTO> resultado = estudianteService.listar(
                null, null, null, null, EstadoLogico.ACTIVO, PageRequest.of(0, 10));

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Listar - Con VER_ESTUDIANTE conserva la urlFoto de cada estudiante")
    void listar_ConVerEstudiante_ConservaUrlFoto() {
        mockAuthConPermiso("1", "VER_ESTUDIANTE");
        resumenDTO.setUrlFoto("/estudiantes/1/foto");
        stubBusqueda();

        Page<EstudianteResumenDTO> resultado = estudianteService.listar(
                null, null, null, null, EstadoLogico.ACTIVO, PageRequest.of(0, 10));

        assertEquals("/estudiantes/1/foto", resultado.getContent().get(0).getUrlFoto());
    }

    @Test
    @DisplayName("Listar - Sin VER_ESTUDIANTE (ej. TUTOR, DOCENTE) oculta la urlFoto")
    void listar_SinVerEstudiante_OcultaUrlFoto() {
        mockAuthConPermiso("1", "VER_ESTUDIANTE_RESUMIDO");
        resumenDTO.setUrlFoto("/estudiantes/1/foto");
        stubBusqueda();

        Page<EstudianteResumenDTO> resultado = estudianteService.listar(
                null, null, null, null, null, PageRequest.of(0, 10));

        assertNull(resultado.getContent().get(0).getUrlFoto());
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
    @DisplayName("Listar - Con filtro de carrera sin BUSCAR_ESTUDIANTE lanza 403 (B5)")
    void listar_ConFiltroCarrera_SinBuscarEstudiante_LanzaForbidden() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO");

        assertThrows(ResponseStatusException.class,
                () -> estudianteService.listar(null, null, 3, null, null, PageRequest.of(0, 10)));
        verifyNoInteractions(estudianteRepository);
    }

    @Test
    @DisplayName("Listar - Sin filtros no exige BUSCAR_ESTUDIANTE (B5)")
    void listar_SinFiltros_NoExigeBuscarEstudiante() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO");
        stubBusqueda();

        assertNotNull(estudianteService.listar(null, null, null, null, null, PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("Listar - Todos los filtros juntos van en una única consulta (antes solo se aplicaba el primero)")
    void listar_TodosLosFiltros_UnaSolaConsulta() {
        mockAuthConPermisos("1", "VER_ESTUDIANTE_RESUMIDO", "VER_ESTUDIANTE", "BUSCAR_ESTUDIANTE");
        stubBusqueda();

        estudianteService.listar("Ana", 1, 2, 3, EstadoLogico.ACTIVO, PageRequest.of(0, 10));

        verify(estudianteRepository).findAll(anySpec(), eq(PageRequest.of(0, 10)));
        verifyNoMoreInteractions(estudianteRepository);
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
        when(rolRepository.findByNombre("ESTUDIANTE")).thenReturn(Optional.of(new Rol("ESTUDIANTE")));
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
        when(rolRepository.findByNombre("ESTUDIANTE")).thenReturn(Optional.of(new Rol("ESTUDIANTE")));
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
    @DisplayName("Editar - Éxito")
    void editar_Exito() {
        EstudianteUpdateDTO dto = new EstudianteUpdateDTO();
        dto.setNombre("Ana");
        dto.setApellido("González");
        dto.setEmail("ana.garcia@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(2000, 3, 15));

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudianteExistente);
        when(estudianteMapper.toDto(any(Estudiante.class))).thenReturn(responseDTO);

        EstudianteResponseDTO resultado = estudianteService.editar(1, dto);

        assertNotNull(resultado);
        verify(estudianteRepository).save(estudianteExistente);
    }

    @Test
    @DisplayName("Editar - Email duplicado (de otro estudiante) → 409")
    void editar_LanzaExcepcion_EmailDuplicado() {
        EstudianteUpdateDTO dto = new EstudianteUpdateDTO();
        dto.setNombre("Ana");
        dto.setApellido("González");
        dto.setEmail("otro.estudiante@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(2000, 3, 15));

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteRepository.existsByEmailAndIdUsuarioNot("otro.estudiante@utec.edu.uy", 1)).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.editar(1, dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar - Da de baja lógica los teléfonos que ya no vienen en la lista")
    void editar_DaDeBajaTelefonosRemovidos() {
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

        estudianteService.editar(1, dto);

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
    @DisplayName("Editar - Reactiva un teléfono previamente eliminado que vuelve a la lista")
    void editar_ReactivaTelefonoEliminado() {
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

        estudianteService.editar(1, dto);

        assertEquals(EstadoLogico.ACTIVO, telefonoEliminado.getEstado());
        assertEquals(1, estudianteExistente.getTelefonos().size());
    }

    @Test
    @DisplayName("Editar - Rechaza asignar un grupo que no está ACTIVO")
    void editar_LanzaExcepcion_GrupoNoActivo() {
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
                () -> estudianteService.editar(1, dto));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar - No encontrado → 404")
    void editar_LanzaExcepcion_NoEncontrado() {
        EstudianteUpdateDTO dto = new EstudianteUpdateDTO();
        dto.setNombre("Ana");
        dto.setApellido("González");
        dto.setEmail("ana.garcia@utec.edu.uy");
        dto.setFechaNacimiento(LocalDate.of(2000, 3, 15));

        when(estudianteRepository.findById(99)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.editar(99, dto));

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
        MiPerfilEstudianteDTO perfil = MiPerfilEstudianteDTO.builder().ciudad("Montevideo").build();
        when(estudianteMapper.toMiPerfilDto(estudianteExistente)).thenReturn(perfil);

        MiPerfilEstudianteDTO resultado = estudianteService.editarMisDatos(1, dto);

        assertSame(perfil, resultado);
        assertEquals("Montevideo", estudianteExistente.getCiudad());
        verify(estudianteRepository).save(estudianteExistente);
        // Nunca la ficha completa: trae el bloque confidencial y el motivo de derivación.
        verify(estudianteMapper, never()).toDto(any());
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
    @DisplayName("Ver mi perfil - Devuelve el perfil propio (con dirección), nunca la ficha completa")
    void verMiPerfil_Exito() {
        mockAuth("1", true);
        MiPerfilEstudianteDTO perfil = MiPerfilEstudianteDTO.builder()
                .nombre("Ana").ciudad("Montevideo").calle("Av. 18 de Julio").nroPuerta(1234).build();

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteMapper.toMiPerfilDto(estudianteExistente)).thenReturn(perfil);

        MiPerfilEstudianteDTO resultado = estudianteService.verMiPerfil();

        assertEquals("Ana", resultado.getNombre());
        assertEquals("Av. 18 de Julio", resultado.getCalle());
        // La ficha completa trae el bloque confidencial y el motivo de derivación.
        verify(estudianteMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Ver mi perfil - El propio estudiante conserva la urlFoto de su foto")
    void verMiPerfil_ConservaUrlFotoPropia() {
        mockAuth("1", true);
        MiPerfilEstudianteDTO perfil = MiPerfilEstudianteDTO.builder().urlFoto("/estudiantes/1/foto").build();

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteMapper.toMiPerfilDto(estudianteExistente)).thenReturn(perfil);

        MiPerfilEstudianteDTO resultado = estudianteService.verMiPerfil();

        assertEquals("/estudiantes/1/foto", resultado.getUrlFoto());
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
    @DisplayName("Restablecer contraseña - Éxito, deja al estudiante PENDIENTE_DE_ACTIVACION")
    void restablecerContrasenia_Exito() {
        // estudianteExistente ya arranca ACTIVO (setUp) — caso realista: un admin
        // resetea la contraseña de un estudiante que ya venía usando el sistema.
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(passwordGeneratorService.generar()).thenReturn("NuevaPassGenerada123!");
        when(passwordEncoder.encode("NuevaPassGenerada123!")).thenReturn("hashedNuevaPass");
        when(estudianteRepository.save(any(Estudiante.class))).thenReturn(estudianteExistente);

        estudianteService.restablecerContrasenia(1);

        assertEquals("hashedNuevaPass", estudianteExistente.getContrasenia());
        // El reset admin obliga al próximo login a elegir una contraseña propia
        // (JwtRequestFilter restringe a un usuario en este estado) — sin esto, un
        // estudiante ya ACTIVO seguiría ACTIVO después del reset y el enforcement
        // nunca se dispararía.
        assertEquals(EstadoLogico.PENDIENTE_DE_ACTIVACION, estudianteExistente.getEstado());
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

    @Test
    @DisplayName("Restablecer contraseña - Estudiante inactivo (409), no lo reactiva silenciosamente")
    void restablecerContrasenia_EstudianteInactivo_LanzaConflict() {
        estudianteExistente.setEstado(EstadoLogico.INACTIVO);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.restablecerContrasenia(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
        verify(emailService, never()).enviarContraseniaRestablecida(any(), any(), any());
    }

    // =========================================================
    // FOTO DE PERFIL
    // =========================================================

    @Test
    @DisplayName("Subir foto - Éxito, guarda la ruta relativa devuelta por FileStorageService")
    void subirFoto_Exito() {
        mockAuthConPermisos("10", "EDITAR_ESTUDIANTE", "VER_BLOQUE_CONFIDENCIAL");
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
        // Con VER_BLOQUE_CONFIDENCIAL la respuesta conserva el bloque confidencial.
        assertEquals("Diagnóstico reservado", resultado.getInformacionSalud());
    }

    @Test
    @DisplayName("Subir foto - El propio estudiante no recibe el bloque confidencial ni el motivo de derivación")
    void subirFoto_PropioEstudiante_OcultaBloqueConfidencial() {
        mockAuth("1", true);
        MultipartFile foto = new MockMultipartFile(
                "foto", "perfil.jpg", "image/jpeg", "contenido".getBytes());

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(fileStorageService.guardar(eq(foto), eq("fotos/1"), anyList(), anyLong()))
                .thenReturn("3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.jpg");
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));
        when(estudianteMapper.toDto(estudianteExistente)).thenReturn(responseDTO);

        EstudianteResponseDTO resultado = estudianteService.subirFoto(1, foto);

        assertNull(resultado.getInformacionSalud());
        assertNull(resultado.getSistemaSalud());
        assertNull(resultado.getMotivoDerivacion());
    }

    @Test
    @DisplayName("Subir foto - Si el save() falla, limpia el archivo físico y restaura la ruta anterior")
    void subirFoto_LimpiaArchivoFisico_SiFallaElSave() {
        estudianteExistente.setFoto("fotos/1/foto-vieja.jpg");
        MultipartFile foto = new MockMultipartFile(
                "foto", "perfil.jpg", "image/jpeg", "contenido".getBytes());

        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(fileStorageService.guardar(eq(foto), eq("fotos/1"), anyList(), anyLong()))
                .thenReturn("3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.jpg");
        when(estudianteRepository.save(any(Estudiante.class)))
                .thenThrow(new DataIntegrityViolationException("constraint violation"));

        assertThrows(DataIntegrityViolationException.class, () -> estudianteService.subirFoto(1, foto));

        assertEquals("fotos/1/foto-vieja.jpg", estudianteExistente.getFoto());
        verify(fileStorageService).eliminarSiExiste("fotos/1/3f2504e0-4f89-4f9c-a9b0-a1b2c3d4e5f6.jpg");
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
    @DisplayName("Desactivar - Estado pasa a INACTIVO")
    void desactivar_DebePonerEstadoInactivo_YGuardar() {
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));
        when(estudianteRepository.save(any(Estudiante.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        estudianteService.desactivar(1);

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

    @Test
    @DisplayName("Reactivar - Estudiante PENDIENTE_DE_ACTIVACION → 409, no saltea el cambio de contraseña obligatorio")
    void reactivar_LanzaExcepcion_PendienteDeActivacion() {
        estudianteExistente.setEstado(EstadoLogico.PENDIENTE_DE_ACTIVACION);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.reactivar(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reactivar - Estudiante ya ACTIVO → 409")
    void reactivar_LanzaExcepcion_YaActivo() {
        estudianteExistente.setEstado(EstadoLogico.ACTIVO);
        when(estudianteRepository.findById(1)).thenReturn(Optional.of(estudianteExistente));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> estudianteService.reactivar(1));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
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