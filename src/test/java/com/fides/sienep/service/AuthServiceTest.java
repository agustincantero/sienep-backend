package com.fides.sienep.service;

import com.fides.sienep.dto.LoginRequestDTO;
import com.fides.sienep.dto.LoginResponseDTO;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.LogIntento;
import com.fides.sienep.model.Rol;
import com.fides.sienep.model.Usuario;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.LogIntentoRepository;
import com.fides.sienep.repository.UsuarioRepository;
import com.fides.sienep.utils.JwtTokenUtil;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private PasswordGeneratorService passwordGeneratorService;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private AuthService authService;

    private Funcionario funcionario;
    private Rol rol;
    private LoginRequestDTO loginDto;
    private static final String IP = "127.0.0.1";

    @BeforeEach
    void setUp() {
        rol = new Rol("ADMINISTRADOR");

        funcionario = new Funcionario();
        funcionario.setIdUsuario(2);
        funcionario.setNombre("Juan");
        funcionario.setEmail("juan.garcia@utec.edu.uy");
        funcionario.setRol(rol);
        funcionario.setBloqueado(false);

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
    @DisplayName("Login - Usuario bloqueado (403), ni siquiera valida contraseña")
    void login_LanzaForbidden_UsuarioBloqueado() {
        funcionario.setBloqueado(true);
        when(funcionarioRepository.findByEmail(any())).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(funcionario));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(loginDto, IP));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
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

    // ── Logout ────────────────────────────────────────────────

    @Test
    @DisplayName("Logout - Usuario autenticado registra auditoría")
    void logout_UsuarioAutenticado_RegistraAuditoria() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("2");
        when(auth.getName()).thenReturn("2");

        authService.logout(auth, IP);

        verify(auditoriaService).registrarEventoManual(eq("SISTEMA"), any(), eq(2), eq(IP));
    }

    @Test
    @DisplayName("Logout - Usuario anónimo no registra auditoría")
    void logout_UsuarioAnonimo_NoRegistraAuditoria() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("anonymousUser");

        authService.logout(auth, IP);

        verifyNoInteractions(auditoriaService);
    }

    @Test
    @DisplayName("Logout - Authentication null no rompe ni audita")
    void logout_AuthenticationNull_NoRompe() {
        assertDoesNotThrow(() -> authService.logout(null, IP));
        verifyNoInteractions(auditoriaService);
    }

    // ── Olvidé mi contraseña ──────────────────────────────────

    @Test
    @DisplayName("Olvidé mi contraseña - Email existe, genera y envía nueva contraseña")
    void olvideMiContrasenia_EmailExiste_GeneraNuevaContrasenia() {
        funcionario.setBloqueado(true); // para confirmar que se desbloquea
        when(usuarioRepository.findByEmail("juan.garcia@utec.edu.uy")).thenReturn(Optional.of(funcionario));
        when(passwordGeneratorService.generar()).thenReturn("Temporal123!");
        when(passwordEncoder.encode("Temporal123!")).thenReturn("hash-fake");

        authService.olvideMiContrasenia("juan.garcia@utec.edu.uy");

        assertEquals("hash-fake", funcionario.getContrasenia());
        assertFalse(funcionario.isBloqueado());
        verify(usuarioRepository).save(funcionario);
        verify(emailService).enviarContraseniaRestablecida(
                "juan.garcia@utec.edu.uy", "Juan", "Temporal123!");
    }

    @Test
    @DisplayName("Olvidé mi contraseña - Email no existe, no revela nada ni lanza excepción")
    void olvideMiContrasenia_EmailNoExiste_NoHaceNada() {
        when(usuarioRepository.findByEmail("inexistente@utec.edu.uy")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.olvideMiContrasenia("inexistente@utec.edu.uy"));

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }
}