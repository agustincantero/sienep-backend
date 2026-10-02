package com.fides.sienep.service;

import com.fides.sienep.dto.AdLoginRequestDTO;
import com.fides.sienep.dto.GoogleLoginRequestDTO;
import com.fides.sienep.dto.LoginRequestDTO;
import com.fides.sienep.dto.LoginResponseDTO;
import com.fides.sienep.dto.UsuarioAutenticadoDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.LogIntento;
import com.fides.sienep.model.Permiso;
import com.fides.sienep.model.Rol;
import com.fides.sienep.model.RolPermiso;
import com.fides.sienep.model.TipoUsuario;
import com.fides.sienep.model.TokenRestablecimiento;
import com.fides.sienep.model.Usuario;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.LogIntentoRepository;
import com.fides.sienep.repository.TokenRestablecimientoRepository;
import com.fides.sienep.repository.UsuarioRepository;
import com.fides.sienep.utils.JwtTokenUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authManager;
    @Mock private JwtTokenUtil jwtTokenUtil;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private LogIntentoRepository logIntentoRepository;
    @Mock private TokenRestablecimientoRepository tokenRestablecimientoRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private AuditoriaService auditoriaService;
    @Mock private PasswordResetRateLimiter passwordResetRateLimiter;
    @Mock private LoginIpRateLimiter loginIpRateLimiter;
    @Mock private GoogleIdTokenVerifierService googleIdTokenVerifierService;
    @Mock private ActiveDirectoryVerifierService activeDirectoryVerifierService;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks
    private AuthService authService;

    private Funcionario funcionario;
    private Rol rol;
    private LoginRequestDTO loginDto;
    private static final String IP = "127.0.0.1";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:3000");
        lenient().when(loginIpRateLimiter.registrarIntento(anyString())).thenReturn(true);
        // El template real abriría una transacción; acá solo hace falta que ejecute el callback
        lenient().when(transactionTemplate.execute(any())).thenAnswer(inv -> inv.<TransactionCallback<?>>getArgument(0).doInTransaction(null));

        rol = new Rol("ADMINISTRADOR");

        funcionario = new Funcionario();
        funcionario.setIdUsuario(2);
        funcionario.setNombre("Juan");
        funcionario.setEmail("juan.garcia@utec.edu.uy");
        funcionario.setRol(rol);
        funcionario.setBloqueado(false);
        funcionario.setEstado(EstadoLogico.ACTIVO);

        loginDto = new LoginRequestDTO();
        loginDto.setEmail("juan.garcia@utec.edu.uy");
        loginDto.setContrasenia("Admin123!");
    }

    // ── Login ─────────────────────────────────────────────────

    @Test
    @DisplayName("Login - Éxito para funcionario, genera token y audita")
    void login_Exito_Funcionario() {
        when(funcionarioRepository.findByEmail("juan.garcia@utec.edu.uy")).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(jwtTokenUtil.generateToken(2, "ADMINISTRADOR")).thenReturn("token-fake");

        LoginResponseDTO resultado = authService.login(loginDto, IP);

        assertEquals("token-fake", resultado.getToken());
        verify(logIntentoRepository).save(argThat((LogIntento l) -> l.getIdUsuario() == 2 && l.isFueExitoso()));
        verify(auditoriaService).registrarEventoManual(eq("SISTEMA"), any(), eq(2), eq(IP));
    }

    @Test
    @DisplayName("Login - Éxito para estudiante, usa rol ESTUDIANTE")
    void login_Exito_Estudiante() {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(1);
        estudiante.setEmail("ana.perez@estudiantes.utec.edu.uy");
        estudiante.setBloqueado(false);
        estudiante.setRol(new Rol("ESTUDIANTE"));
        loginDto.setEmail("ana.perez@estudiantes.utec.edu.uy");

        when(funcionarioRepository.findByEmail("ana.perez@estudiantes.utec.edu.uy")).thenReturn(Optional.empty());
        when(estudianteRepository.findByEmail("ana.perez@estudiantes.utec.edu.uy")).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(jwtTokenUtil.generateToken(1, "ESTUDIANTE")).thenReturn("token-estudiante");

        LoginResponseDTO resultado = authService.login(loginDto, IP);

        assertEquals("token-estudiante", resultado.getToken());
        verify(jwtTokenUtil).generateToken(1, "ESTUDIANTE");
    }

    @Test
    @DisplayName("Login - Email en mayúsculas se normaliza a minúsculas antes de buscar")
    void login_NormalizaEmailAMinusculas() {
        loginDto.setEmail("JUAN.GARCIA@UTEC.EDU.UY");

        when(funcionarioRepository.findByEmail("juan.garcia@utec.edu.uy")).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(jwtTokenUtil.generateToken(2, "ADMINISTRADOR")).thenReturn("token-fake");

        LoginResponseDTO resultado = authService.login(loginDto, IP);

        assertEquals("token-fake", resultado.getToken());
        verify(funcionarioRepository).findByEmail("juan.garcia@utec.edu.uy");
        verify(authManager).authenticate(argThat(auth ->
                "juan.garcia@utec.edu.uy".equals(auth.getPrincipal())));
    }

    @Test
    @DisplayName("Login - Email no existe en ningún lado (401)")
    void login_LanzaUnauthorized_EmailNoExiste() {
        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(estudianteRepository.findByEmail(any())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(loginDto, IP));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(jwtTokenUtil);
    }

    @Test
    @DisplayName("Login - Email no existe igual ejecuta un matches() dummy (mitigación de timing side-channel)")
    void login_EmailNoExiste_EjecutaMatchesDummyParaIgualarTiempos() {
        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(estudianteRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> authService.login(loginDto, IP));

        // No debe importar el resultado (siempre false), solo que se pague el mismo
        // costo de BCrypt que en el camino de "contraseña incorrecta" — de lo
        // contrario un atacante puede distinguir "email no existe" por timing.
        verify(passwordEncoder).matches(eq(loginDto.getContrasenia()), anyString());
        verifyNoInteractions(authManager);
    }

    @Test
    @DisplayName("Login - Usuario bloqueado (401 igual que credenciales inválidas, B20), ni siquiera valida contraseña")
    void login_LanzaUnauthorized_UsuarioBloqueado() {
        funcionario.setBloqueado(true);
        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(loginDto, IP));

        // Mismo status que "no existe"/"contraseña incorrecta": no debe distinguirse
        // una cuenta bloqueada de una inexistente solo mirando el código de respuesta.
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(authManager);
        verifyNoInteractions(jwtTokenUtil);
    }

    @Test
    @DisplayName("Login - Contraseña incorrecta registra intento fallido (401), sin bloquear todavía")
    void login_CredencialesInvalidas_RegistraIntentoFallido() {
        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        doThrow(new BadCredentialsException("Credenciales inválidas"))
                .when(authManager).authenticate(any());
        when(logIntentoRepository.contarIntentosFallidosDesde(eq(2), any())).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(loginDto, IP));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(logIntentoRepository).save(argThat((LogIntento l) -> l.getIdUsuario() == 2 && !l.isFueExitoso()));
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(jwtTokenUtil);
    }

    @Test
    @DisplayName("Login - Quinto intento fallido en la ventana bloquea el usuario")
    void login_QuintoIntentoFallido_BloqueaUsuario() {
        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        doThrow(new BadCredentialsException("Credenciales inválidas"))
                .when(authManager).authenticate(any());
        when(logIntentoRepository.contarIntentosFallidosDesde(eq(2), any())).thenReturn(5L);

        assertThrows(ResponseStatusException.class, () -> authService.login(loginDto, IP));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertTrue(captor.getValue().isBloqueado());
    }

    @Test
    @DisplayName("Login - Límite de intentos fallidos por IP excedido (429), sin importar la cuenta")
    void login_LimiteDeIpExcedido_LanzaTooManyRequests() {
        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        doThrow(new BadCredentialsException("Credenciales inválidas"))
                .when(authManager).authenticate(any());
        when(logIntentoRepository.contarIntentosFallidosDesde(eq(2), any())).thenReturn(1L);
        when(loginIpRateLimiter.registrarIntento(IP)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(loginDto, IP));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    // ── Login con Google ─────────────────────────────────────────

    private GoogleLoginRequestDTO googleDto() {
        GoogleLoginRequestDTO dto = new GoogleLoginRequestDTO();
        dto.setIdToken("id-token-fake");
        return dto;
    }

    @Test
    @DisplayName("Login con Google - Éxito para funcionario, genera token y audita")
    void loginConGoogle_Exito_Funcionario() {
        when(googleIdTokenVerifierService.emailVerificado("id-token-fake"))
                .thenReturn(Optional.of("juan.garcia@utec.edu.uy"));
        when(funcionarioRepository.findByEmail("juan.garcia@utec.edu.uy")).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(jwtTokenUtil.generateToken(2, "ADMINISTRADOR")).thenReturn("token-fake");

        LoginResponseDTO resultado = authService.loginConGoogle(googleDto(), IP);

        assertEquals("token-fake", resultado.getToken());
        verify(logIntentoRepository).save(argThat((LogIntento l) -> l.getIdUsuario() == 2 && l.isFueExitoso()));
        verify(auditoriaService).registrarEventoManual(eq("SISTEMA"), any(), eq(2), eq(IP));
        verifyNoInteractions(authManager);
    }

    @Test
    @DisplayName("Login con Google - Éxito para estudiante, usa rol ESTUDIANTE")
    void loginConGoogle_Exito_Estudiante() {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(1);
        estudiante.setEmail("ana.perez@estudiantes.utec.edu.uy");
        estudiante.setBloqueado(false);
        estudiante.setEstado(EstadoLogico.ACTIVO);
        estudiante.setRol(new Rol("ESTUDIANTE"));

        when(googleIdTokenVerifierService.emailVerificado("id-token-fake"))
                .thenReturn(Optional.of("ana.perez@estudiantes.utec.edu.uy"));
        when(funcionarioRepository.findByEmail("ana.perez@estudiantes.utec.edu.uy")).thenReturn(Optional.empty());
        when(estudianteRepository.findByEmail("ana.perez@estudiantes.utec.edu.uy")).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(jwtTokenUtil.generateToken(1, "ESTUDIANTE")).thenReturn("token-estudiante");

        LoginResponseDTO resultado = authService.loginConGoogle(googleDto(), IP);

        assertEquals("token-estudiante", resultado.getToken());
        verify(jwtTokenUtil).generateToken(1, "ESTUDIANTE");
    }

    @Test
    @DisplayName("Login con Google - ID token inválido o no verificado por Google (401), no consulta cuentas")
    void loginConGoogle_TokenInvalido_LanzaUnauthorized() {
        when(googleIdTokenVerifierService.emailVerificado("id-token-fake")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.loginConGoogle(googleDto(), IP));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(funcionarioRepository, estudianteRepository, jwtTokenUtil);
    }

    @Test
    @DisplayName("Login con Google - Email verificado no pertenece a ninguna cuenta (401), no da de alta nada")
    void loginConGoogle_EmailSinCuenta_LanzaUnauthorized() {
        when(googleIdTokenVerifierService.emailVerificado("id-token-fake"))
                .thenReturn(Optional.of("desconocido@utec.edu.uy"));
        when(funcionarioRepository.findByEmail("desconocido@utec.edu.uy")).thenReturn(Optional.empty());
        when(estudianteRepository.findByEmail("desconocido@utec.edu.uy")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.loginConGoogle(googleDto(), IP));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(jwtTokenUtil);
    }

    @Test
    @DisplayName("Login con Google - Cuenta inactiva (401)")
    void loginConGoogle_CuentaInactiva_LanzaUnauthorized() {
        funcionario.setEstado(EstadoLogico.INACTIVO);
        when(googleIdTokenVerifierService.emailVerificado("id-token-fake"))
                .thenReturn(Optional.of("juan.garcia@utec.edu.uy"));
        when(funcionarioRepository.findByEmail("juan.garcia@utec.edu.uy")).thenReturn(Optional.of(funcionario));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.loginConGoogle(googleDto(), IP));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(jwtTokenUtil);
    }

    @Test
    @DisplayName("Login con Google - Usuario bloqueado (401)")
    void loginConGoogle_UsuarioBloqueado_LanzaUnauthorized() {
        funcionario.setBloqueado(true);
        when(googleIdTokenVerifierService.emailVerificado("id-token-fake"))
                .thenReturn(Optional.of("juan.garcia@utec.edu.uy"));
        when(funcionarioRepository.findByEmail("juan.garcia@utec.edu.uy")).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.loginConGoogle(googleDto(), IP));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(jwtTokenUtil);
    }

    // ── Login con Active Directory ───────────────────────────────

    private AdLoginRequestDTO adDto() {
        AdLoginRequestDTO dto = new AdLoginRequestDTO();
        dto.setUsuario("jgarcia");
        dto.setContrasenia("Corp123!");
        return dto;
    }

    @Test
    @DisplayName("Login con AD - Éxito para funcionario activo, genera token y audita")
    void loginConAd_Exito_Funcionario() {
        when(activeDirectoryVerifierService.emailVerificado("jgarcia", "Corp123!"))
                .thenReturn(Optional.of("juan.garcia@utec.edu.uy"));
        when(funcionarioRepository.findByEmail("juan.garcia@utec.edu.uy")).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(jwtTokenUtil.generateToken(2, "ADMINISTRADOR")).thenReturn("token-fake");

        LoginResponseDTO resultado = authService.loginConAd(adDto(), IP);

        assertEquals("token-fake", resultado.getToken());
        verify(logIntentoRepository).save(argThat((LogIntento l) -> l.getIdUsuario() == 2 && l.isFueExitoso()));
        verify(auditoriaService).registrarEventoManual(eq("SISTEMA"), any(), eq(2), eq(IP));
        verifyNoInteractions(authManager);
    }

    @Test
    @DisplayName("Login con AD - Éxito para estudiante, usa rol ESTUDIANTE")
    void loginConAd_Exito_Estudiante() {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(1);
        estudiante.setEmail("ana.perez@estudiantes.utec.edu.uy");
        estudiante.setBloqueado(false);
        estudiante.setEstado(EstadoLogico.ACTIVO);
        estudiante.setRol(new Rol("ESTUDIANTE"));

        when(activeDirectoryVerifierService.emailVerificado("aperez", "Corp123!"))
                .thenReturn(Optional.of("ana.perez@estudiantes.utec.edu.uy"));
        when(funcionarioRepository.findByEmail("ana.perez@estudiantes.utec.edu.uy")).thenReturn(Optional.empty());
        when(estudianteRepository.findByEmail("ana.perez@estudiantes.utec.edu.uy")).thenReturn(Optional.of(estudiante));
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(estudiante));
        when(jwtTokenUtil.generateToken(1, "ESTUDIANTE")).thenReturn("token-estudiante");

        AdLoginRequestDTO dto = adDto();
        dto.setUsuario("aperez");

        LoginResponseDTO resultado = authService.loginConAd(dto, IP);

        assertEquals("token-estudiante", resultado.getToken());
        verify(jwtTokenUtil).generateToken(1, "ESTUDIANTE");
    }

    @Test
    @DisplayName("Login con AD - Cuenta PENDIENTE_DE_ACTIVACION sigue pendiente (hay que forzar el cambio de contraseña)")
    void loginConAd_CuentaPendiente_SiguePendienteDeActivacion() {
        funcionario.setEstado(EstadoLogico.PENDIENTE_DE_ACTIVACION);
        when(activeDirectoryVerifierService.emailVerificado("jgarcia", "Corp123!"))
                .thenReturn(Optional.of("juan.garcia@utec.edu.uy"));
        when(funcionarioRepository.findByEmail("juan.garcia@utec.edu.uy")).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(jwtTokenUtil.generateToken(2, "ADMINISTRADOR")).thenReturn("token-fake");

        authService.loginConAd(adDto(), IP);

        assertEquals(EstadoLogico.PENDIENTE_DE_ACTIVACION, funcionario.getEstado());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login con AD - Credenciales rechazadas por AD (401), no consulta cuentas")
    void loginConAd_CredencialesRechazadas_LanzaUnauthorized() {
        when(activeDirectoryVerifierService.emailVerificado("jgarcia", "Corp123!")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.loginConAd(adDto(), IP));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(funcionarioRepository, estudianteRepository, jwtTokenUtil);
    }

    @Test
    @DisplayName("Login con AD - Email verificado no pertenece a ninguna cuenta (401), no da de alta nada")
    void loginConAd_EmailSinCuenta_LanzaUnauthorized() {
        when(activeDirectoryVerifierService.emailVerificado("jgarcia", "Corp123!"))
                .thenReturn(Optional.of("desconocido@utec.edu.uy"));
        when(funcionarioRepository.findByEmail("desconocido@utec.edu.uy")).thenReturn(Optional.empty());
        when(estudianteRepository.findByEmail("desconocido@utec.edu.uy")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.loginConAd(adDto(), IP));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(jwtTokenUtil);
    }

    @Test
    @DisplayName("Login con AD - Cuenta inactiva (401)")
    void loginConAd_CuentaInactiva_LanzaUnauthorized() {
        funcionario.setEstado(EstadoLogico.INACTIVO);
        when(activeDirectoryVerifierService.emailVerificado("jgarcia", "Corp123!"))
                .thenReturn(Optional.of("juan.garcia@utec.edu.uy"));
        when(funcionarioRepository.findByEmail("juan.garcia@utec.edu.uy")).thenReturn(Optional.of(funcionario));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.loginConAd(adDto(), IP));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(jwtTokenUtil);
    }

    @Test
    @DisplayName("Login con AD - Usuario bloqueado (401)")
    void loginConAd_UsuarioBloqueado_LanzaUnauthorized() {
        funcionario.setBloqueado(true);
        when(activeDirectoryVerifierService.emailVerificado("jgarcia", "Corp123!"))
                .thenReturn(Optional.of("juan.garcia@utec.edu.uy"));
        when(funcionarioRepository.findByEmail("juan.garcia@utec.edu.uy")).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.loginConAd(adDto(), IP));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verifyNoInteractions(jwtTokenUtil);
    }

    @Test
    @DisplayName("Login con AD - AD no configurado o controlador de dominio inalcanzable (503)")
    void loginConAd_AdNoDisponible_Lanza503() {
        when(activeDirectoryVerifierService.emailVerificado("jgarcia", "Corp123!"))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AD no configurado"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.loginConAd(adDto(), IP));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        verifyNoInteractions(funcionarioRepository, estudianteRepository, jwtTokenUtil);
    }

    // ── Logout ────────────────────────────────────────────────

    @Test
    @DisplayName("Logout - Usuario autenticado registra auditoría e invalida tokens ya emitidos")
    void logout_UsuarioAutenticado_RegistraAuditoriaEInvalidaTokens() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("2");
        when(auth.getName()).thenReturn("2");
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));

        LocalDateTime antes = funcionario.getCredencialesVigentesDesde();
        authService.logout(auth, IP);

        verify(auditoriaService).registrarEventoManual(eq("SISTEMA"), any(), eq(2), eq(IP));
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertNotEquals(antes, captor.getValue().getCredencialesVigentesDesde());
    }

    @Test
    @DisplayName("Logout - Si falla la auditoría, el token igual queda invalidado y el logout no falla")
    void logout_FallaAuditoria_InvalidaIgualYNoPropaga() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("2");
        when(auth.getName()).thenReturn("2");
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        doThrow(new RuntimeException("falló la auditoría"))
                .when(auditoriaService).registrarEventoManual(any(), any(), any(), any());

        assertDoesNotThrow(() -> authService.logout(auth, IP));

        verify(usuarioRepository).save(funcionario);
    }

    @Test
    @DisplayName("Logout - Si no se puede invalidar el token, el error se propaga y no se audita")
    void logout_FallaInvalidacion_Propaga() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("2");
        when(auth.getName()).thenReturn("2");
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.save(any(Usuario.class))).thenThrow(new RuntimeException("falló el guardado"));

        assertThrows(RuntimeException.class, () -> authService.logout(auth, IP));

        verifyNoInteractions(auditoriaService);
    }

    @Test
    @DisplayName("Logout - Usuario anónimo no registra auditoría ni toca el usuario")
    void logout_UsuarioAnonimo_NoRegistraAuditoria() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("anonymousUser");

        authService.logout(auth, IP);

        verifyNoInteractions(auditoriaService);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("Logout - Authentication null no rompe ni audita")
    void logout_AuthenticationNull_NoRompe() {
        assertDoesNotThrow(() -> authService.logout(null, IP));
        verifyNoInteractions(auditoriaService);
        verifyNoInteractions(usuarioRepository);
    }

    // ── Olvidé mi contraseña ──────────────────────────────────

    @Test
    @DisplayName("Olvidé mi contraseña - Email existe, genera token y manda el link (no toca la contraseña)")
    void olvideMiContrasenia_EmailExiste_GeneraTokenYEnviaLink() {
        when(passwordResetRateLimiter.registrarIntento("juan.garcia@utec.edu.uy")).thenReturn(true);
        when(usuarioRepository.findByEmail("juan.garcia@utec.edu.uy")).thenReturn(Optional.of(funcionario));

        authService.olvideMiContrasenia("juan.garcia@utec.edu.uy");

        // La contraseña actual NO se toca acá — recién se aplica al confirmar el token
        verify(usuarioRepository, never()).save(any());
        verify(tokenRestablecimientoRepository).invalidarTokensActivos(2);
        verify(tokenRestablecimientoRepository).save(argThat((TokenRestablecimiento t) ->
                t.getIdUsuario() == 2 && !t.isUsado() && t.getFechaExpiracion().isAfter(LocalDateTime.now())));
        verify(emailService).enviarLinkRestablecimiento(
                eq("juan.garcia@utec.edu.uy"), eq("Juan"),
                argThat(link -> link.startsWith("http://localhost:3000/reset-password?token=")));
    }

    @Test
    @DisplayName("Olvidé mi contraseña - Email no existe, no revela nada ni lanza excepción")
    void olvideMiContrasenia_EmailNoExiste_NoHaceNada() {
        when(passwordResetRateLimiter.registrarIntento("inexistente@utec.edu.uy")).thenReturn(true);
        when(usuarioRepository.findByEmail("inexistente@utec.edu.uy")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.olvideMiContrasenia("inexistente@utec.edu.uy"));

        verifyNoInteractions(tokenRestablecimientoRepository, emailService);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Olvidé mi contraseña - Fallo de email no lanza excepción (no debe revelar nada distinto)")
    void olvideMiContrasenia_FalloDeEmail_NoLanza() {
        when(passwordResetRateLimiter.registrarIntento("juan.garcia@utec.edu.uy")).thenReturn(true);
        when(usuarioRepository.findByEmail("juan.garcia@utec.edu.uy")).thenReturn(Optional.of(funcionario));
        doThrow(new RuntimeException("SMTP no disponible"))
                .when(emailService).enviarLinkRestablecimiento(any(), any(), any());

        assertDoesNotThrow(() -> authService.olvideMiContrasenia("juan.garcia@utec.edu.uy"));

        verify(tokenRestablecimientoRepository).save(any(TokenRestablecimiento.class));
    }

    @Test
    @DisplayName("Olvidé mi contraseña - Límite excedido con email existente (429), no genera token")
    void olvideMiContrasenia_LanzaTooManyRequests_EmailExiste() {
        when(passwordResetRateLimiter.registrarIntento("juan.garcia@utec.edu.uy")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.olvideMiContrasenia("juan.garcia@utec.edu.uy"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verifyNoInteractions(usuarioRepository, tokenRestablecimientoRepository, emailService);
    }

    @Test
    @DisplayName("Olvidé mi contraseña - Límite excedido con email inexistente (429 igual, no filtra existencia)")
    void olvideMiContrasenia_LanzaTooManyRequests_EmailNoExiste() {
        when(passwordResetRateLimiter.registrarIntento("inexistente@utec.edu.uy")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.olvideMiContrasenia("inexistente@utec.edu.uy"));

        // Misma respuesta (429) que con un email existente: el rate-limit no debe
        // filtrar si el email pertenece a un usuario real o no.
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verifyNoInteractions(usuarioRepository, tokenRestablecimientoRepository, emailService);
    }

    // ── Restablecer contraseña con token ───────────────────────

    @Test
    @DisplayName("Restablecer con token - Éxito, aplica la contraseña elegida y marca el token usado")
    void restablecerConToken_Exito() {
        TokenRestablecimiento token = new TokenRestablecimiento(2, "hash-fake", LocalDateTime.now().plusMinutes(10));

        when(tokenRestablecimientoRepository.findByTokenHashAndUsadoFalse(anyString()))
                .thenReturn(Optional.of(token));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(passwordEncoder.encode("NuevaPass123!")).thenReturn("hash-nueva");

        authService.restablecerContraseniaConToken("token-plano", "NuevaPass123!");

        assertEquals("hash-nueva", funcionario.getContrasenia());
        assertFalse(funcionario.isBloqueado());
        assertTrue(token.isUsado());
        // Ya estaba ACTIVO — este camino no debería tocar el estado en ese caso.
        assertEquals(EstadoLogico.ACTIVO, funcionario.getEstado());
        verify(usuarioRepository).save(funcionario);
        verify(tokenRestablecimientoRepository).save(token);
    }

    @Test
    @DisplayName("Restablecer con token - Estado PENDIENTE_DE_ACTIVACION pasa a ACTIVO")
    void restablecerConToken_ActivaEstadoPendiente() {
        funcionario.setEstado(EstadoLogico.PENDIENTE_DE_ACTIVACION);
        TokenRestablecimiento token = new TokenRestablecimiento(2, "hash-fake", LocalDateTime.now().plusMinutes(10));

        when(tokenRestablecimientoRepository.findByTokenHashAndUsadoFalse(anyString()))
                .thenReturn(Optional.of(token));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));
        when(passwordEncoder.encode("NuevaPass123!")).thenReturn("hash-nueva");

        // Cubre el caso de alguien PENDIENTE_DE_ACTIVACION que pierde la contraseña
        // temporal y usa "olvidé mi contraseña" en vez de cambiarla desde adentro —
        // sin esto, quedaría en loop infinito en la pantalla obligatoria del frontend
        // pese a tener ya una contraseña propia y funcional.
        authService.restablecerContraseniaConToken("token-plano", "NuevaPass123!");

        assertEquals(EstadoLogico.ACTIVO, funcionario.getEstado());
        verify(usuarioRepository).save(funcionario);
    }

    @Test
    @DisplayName("Restablecer con token - Token inexistente o ya usado (401)")
    void restablecerConToken_LanzaUnauthorized_TokenNoEncontrado() {
        when(tokenRestablecimientoRepository.findByTokenHashAndUsadoFalse(anyString()))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.restablecerContraseniaConToken("token-invalido", "NuevaPass123!"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Restablecer con token - Token vencido (401)")
    void restablecerConToken_LanzaUnauthorized_TokenVencido() {
        TokenRestablecimiento token = new TokenRestablecimiento(2, "hash-fake", LocalDateTime.now().minusMinutes(1));
        when(tokenRestablecimientoRepository.findByTokenHashAndUsadoFalse(anyString()))
                .thenReturn(Optional.of(token));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.restablecerContraseniaConToken("token-vencido", "NuevaPass123!"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(usuarioRepository, never()).save(any());
    }

    // ── Usuario autenticado (GET /auth/me) ────────────────────

    @AfterEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(int idUsuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(idUsuario), null));
    }

    @Test
    @DisplayName("usuarioAutenticado - Funcionario: mapea identidad, rol y tipo FUNCIONARIO")
    void usuarioAutenticado_Funcionario_MapeaTodo() {
        autenticarComo(2);
        funcionario.setApellido("García");
        rol.agregarPermiso(new Permiso("VER_FUNCIONARIOS", "d"));
        rol.agregarPermiso(new Permiso("CREAR_INSTANCIA", "d"));
        when(funcionarioRepository.findByIdConRolYPermisos(2)).thenReturn(Optional.of(funcionario));

        UsuarioAutenticadoDTO dto = authService.usuarioAutenticado();

        assertEquals(2, dto.getIdUsuario());
        assertEquals(TipoUsuario.FUNCIONARIO, dto.getTipo());
        assertEquals("juan.garcia@utec.edu.uy", dto.getEmail());
        assertEquals("Juan", dto.getNombre());
        assertEquals("García", dto.getApellido());
        assertEquals(EstadoLogico.ACTIVO, dto.getEstado());
        assertEquals("ADMINISTRADOR", dto.getRol());
        assertEquals(List.of("CREAR_INSTANCIA", "VER_FUNCIONARIOS"), dto.getPermisos());
        assertNull(dto.getUrlFoto());
        verifyNoInteractions(estudianteRepository);
    }

    @Test
    @DisplayName("usuarioAutenticado - Funcionario con foto: urlFoto apunta a /funcionarios/{id}/foto")
    void usuarioAutenticado_FuncionarioConFoto_UrlFoto() {
        autenticarComo(2);
        funcionario.setFoto("fotos/2/abc.png");
        when(funcionarioRepository.findByIdConRolYPermisos(2)).thenReturn(Optional.of(funcionario));

        assertEquals("/funcionarios/2/foto", authService.usuarioAutenticado().getUrlFoto());
    }

    @Test
    @DisplayName("usuarioAutenticado - Si no es funcionario, resuelve como ESTUDIANTE")
    void usuarioAutenticado_Estudiante_TipoEstudiante() {
        autenticarComo(7);
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(7);
        estudiante.setNombre("Ana");
        estudiante.setApellido("Pérez");
        estudiante.setEmail("ana.perez@utec.edu.uy");
        estudiante.setEstado(EstadoLogico.ACTIVO);
        Rol rolEst = new Rol("ESTUDIANTE");
        rolEst.agregarPermiso(new Permiso("VER_COMENTARIO_NORMAL_ESTUDIANTE", "d"));
        estudiante.setRol(rolEst);
        when(funcionarioRepository.findByIdConRolYPermisos(7)).thenReturn(Optional.empty());
        when(estudianteRepository.findByIdConRolYPermisos(7)).thenReturn(Optional.of(estudiante));

        UsuarioAutenticadoDTO dto = authService.usuarioAutenticado();

        assertEquals(TipoUsuario.ESTUDIANTE, dto.getTipo());
        assertEquals("ESTUDIANTE", dto.getRol());
        assertEquals(List.of("VER_COMENTARIO_NORMAL_ESTUDIANTE"), dto.getPermisos());
        assertNull(dto.getUrlFoto());
    }

    @Test
    @DisplayName("usuarioAutenticado - Estudiante con foto: urlFoto apunta a /estudiantes/{id}/foto")
    void usuarioAutenticado_EstudianteConFoto_UrlFoto() {
        autenticarComo(7);
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(7);
        estudiante.setEstado(EstadoLogico.ACTIVO);
        estudiante.setFoto("fotos/7/abc.jpg");
        estudiante.setRol(new Rol("ESTUDIANTE"));
        when(funcionarioRepository.findByIdConRolYPermisos(7)).thenReturn(Optional.empty());
        when(estudianteRepository.findByIdConRolYPermisos(7)).thenReturn(Optional.of(estudiante));

        assertEquals("/estudiantes/7/foto", authService.usuarioAutenticado().getUrlFoto());
    }

    @Test
    @DisplayName("usuarioAutenticado - permisos: descarta RolPermiso no ACTIVO, deduplica y ordena")
    void usuarioAutenticado_FiltraDeduplicaYOrdenaPermisos() {
        autenticarComo(2);
        Rol r = new Rol("COORDINADOR");
        r.agregarPermiso(new Permiso("VER_B", "d"));
        r.agregarPermiso(new Permiso("VER_A", "d"));
        r.agregarPermiso(new Permiso("VER_A", "d")); // duplicado
        r.getRolPermisos().add(new RolPermiso(r, new Permiso("VER_Z_INACTIVO", "d"), EstadoLogico.INACTIVO));
        funcionario.setRol(r);
        when(funcionarioRepository.findByIdConRolYPermisos(2)).thenReturn(Optional.of(funcionario));

        UsuarioAutenticadoDTO dto = authService.usuarioAutenticado();

        assertEquals(List.of("VER_A", "VER_B"), dto.getPermisos());
    }

    @Test
    @DisplayName("usuarioAutenticado - Rol sin permisos resuelve igual con permisos = []")
    void usuarioAutenticado_RolSinPermisos_PermisosVacio() {
        autenticarComo(2);
        funcionario.setRol(new Rol("SIN_PERMISOS"));
        when(funcionarioRepository.findByIdConRolYPermisos(2)).thenReturn(Optional.of(funcionario));

        UsuarioAutenticadoDTO dto = authService.usuarioAutenticado();

        assertEquals(List.of(), dto.getPermisos());
        assertEquals("SIN_PERMISOS", dto.getRol());
    }

    @Test
    @DisplayName("usuarioAutenticado - Id del token sin funcionario ni estudiante (401)")
    void usuarioAutenticado_TokenSinUsuario_Lanza401() {
        autenticarComo(999);
        when(funcionarioRepository.findByIdConRolYPermisos(999)).thenReturn(Optional.empty());
        when(estudianteRepository.findByIdConRolYPermisos(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.usuarioAutenticado());

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}