package com.fides.sienep.service;

import com.fides.sienep.dto.AtenderSolicitudInstanciaDTO;
import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.dto.RechazarSolicitudInstanciaDTO;
import com.fides.sienep.dto.SolicitudInstanciaRequestDTO;
import com.fides.sienep.dto.SolicitudInstanciaResponseDTO;
import com.fides.sienep.mapper.SolicitudInstanciaMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.EstadoSolicitud;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.model.SolicitudInstancia;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.repository.SolicitudInstanciaRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitudInstanciaServiceTest {

    @Mock private SolicitudInstanciaRepository solicitudRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private InstanciaComunRepository instanciaComunRepository;
    @Mock private InstanciaComunService instanciaComunService;
    @Mock private SolicitudInstanciaMapper solicitudMapper;

    @InjectMocks
    private SolicitudInstanciaService solicitudService;

    private Estudiante estudiante;
    private Funcionario funcionario;
    private SolicitudInstancia pendiente;
    private final SolicitudInstanciaResponseDTO responseDTO = SolicitudInstanciaResponseDTO.builder().build();

    @BeforeEach
    void setUp() {
        estudiante = new Estudiante();
        estudiante.setIdUsuario(5);
        estudiante.setEstado(EstadoLogico.ACTIVO);

        funcionario = new Funcionario();
        funcionario.setIdUsuario(10);

        pendiente = new SolicitudInstancia(estudiante, "Necesito hablar sobre los parciales");
        pendiente.setIdSolicitud(1);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String idUsuario, String... authorities) {
        List<SimpleGrantedAuthority> lista = new ArrayList<>();
        for (String a : authorities) lista.add(new SimpleGrantedAuthority(a));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(idUsuario, null, lista));
    }

    private SolicitudInstanciaRequestDTO request(String motivo) {
        SolicitudInstanciaRequestDTO dto = new SolicitudInstanciaRequestDTO();
        dto.setMotivo(motivo);
        return dto;
    }

    // ── Crear ─────────────────────────────────────────────────

    @Test
    @DisplayName("Crear - Éxito: queda PENDIENTE para el estudiante autenticado, con el motivo recortado")
    void crear_Exito() {
        autenticarComo("5", "ROLE_ESTUDIANTE");
        when(estudianteRepository.findById(5)).thenReturn(Optional.of(estudiante));
        when(solicitudRepository.countByEstudianteIdUsuarioAndEstado(5, EstadoSolicitud.PENDIENTE)).thenReturn(0L);
        when(solicitudRepository.save(any(SolicitudInstancia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(solicitudMapper.toDto(any(SolicitudInstancia.class))).thenReturn(responseDTO);

        assertSame(responseDTO, solicitudService.crear(request("  Quiero una reunión  ")));

        ArgumentCaptor<SolicitudInstancia> captor = ArgumentCaptor.forClass(SolicitudInstancia.class);
        verify(solicitudRepository).save(captor.capture());
        assertSame(estudiante, captor.getValue().getEstudiante());
        assertEquals("Quiero una reunión", captor.getValue().getMotivo());
        assertEquals(EstadoSolicitud.PENDIENTE, captor.getValue().getEstado());
    }

    @Test
    @DisplayName("Crear - Con el máximo de pendientes → 409, no guarda")
    void crear_MaximoDePendientes_409() {
        autenticarComo("5", "ROLE_ESTUDIANTE");
        when(estudianteRepository.findById(5)).thenReturn(Optional.of(estudiante));
        when(solicitudRepository.countByEstudianteIdUsuarioAndEstado(5, EstadoSolicitud.PENDIENTE))
                .thenReturn((long) SolicitudInstanciaService.MAX_PENDIENTES_POR_ESTUDIANTE);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> solicitudService.crear(request("Otra más")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear - Estudiante no activo → 422, no guarda")
    void crear_EstudianteNoActivo_422() {
        autenticarComo("5", "ROLE_ESTUDIANTE");
        estudiante.setEstado(EstadoLogico.INACTIVO);
        when(estudianteRepository.findById(5)).thenReturn(Optional.of(estudiante));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> solicitudService.crear(request("Quiero una reunión")));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(solicitudRepository, never()).save(any());
    }

    // ── Ver una ───────────────────────────────────────────────

    @Test
    @DisplayName("Ver - El estudiante puede ver una solicitud propia")
    void obtenerPorId_Propia() {
        autenticarComo("5", "ROLE_ESTUDIANTE");
        when(solicitudRepository.findById(1)).thenReturn(Optional.of(pendiente));
        when(solicitudMapper.toDto(pendiente)).thenReturn(responseDTO);

        assertSame(responseDTO, solicitudService.obtenerPorId(1));
    }

    @Test
    @DisplayName("Ver - Un estudiante no puede ver la solicitud de otro (403)")
    void obtenerPorId_Ajena_403() {
        autenticarComo("6", "ROLE_ESTUDIANTE");
        when(solicitudRepository.findById(1)).thenReturn(Optional.of(pendiente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> solicitudService.obtenerPorId(1));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(solicitudMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Ver - Un funcionario con VER_SOLICITUDES_INSTANCIA puede ver cualquiera")
    void obtenerPorId_FuncionarioConPermiso() {
        autenticarComo("10", "VER_SOLICITUDES_INSTANCIA");
        when(solicitudRepository.findById(1)).thenReturn(Optional.of(pendiente));
        when(solicitudMapper.toDto(pendiente)).thenReturn(responseDTO);

        assertSame(responseDTO, solicitudService.obtenerPorId(1));
    }

    // ── Atender ───────────────────────────────────────────────

    @Test
    @DisplayName("Atender - Crea la instancia para el estudiante de la solicitud y la deja ATENDIDA y enlazada")
    void atender_Exito() {
        autenticarComo("10", "ATENDER_SOLICITUD_INSTANCIA", "CREAR_INSTANCIA");
        LocalDateTime fecha = LocalDateTime.now().plusDays(2);
        AtenderSolicitudInstanciaDTO dto = new AtenderSolicitudInstanciaDTO();
        dto.setIdCategoria(3);
        dto.setFechaHora(fecha);
        dto.setRespuesta("Te esperamos en la sala 2");

        InstanciaComunResponseDTO creada = new InstanciaComunResponseDTO();
        creada.setCodInstancia(77);
        InstanciaComun referencia = new InstanciaComun();

        when(solicitudRepository.findByIdParaActualizar(1)).thenReturn(Optional.of(pendiente));
        when(instanciaComunService.crear(any(InstanciaComunRequestDTO.class))).thenReturn(creada);
        when(funcionarioRepository.findById(10)).thenReturn(Optional.of(funcionario));
        when(instanciaComunRepository.getReferenceById(77)).thenReturn(referencia);
        when(solicitudRepository.save(pendiente)).thenReturn(pendiente);
        when(solicitudMapper.toDto(pendiente)).thenReturn(responseDTO);

        assertSame(responseDTO, solicitudService.atender(1, dto));

        ArgumentCaptor<InstanciaComunRequestDTO> captor = ArgumentCaptor.forClass(InstanciaComunRequestDTO.class);
        verify(instanciaComunService).crear(captor.capture());
        assertEquals(5, captor.getValue().getIdEstudiante());
        assertEquals(3, captor.getValue().getIdCategoria());
        assertEquals(fecha, captor.getValue().getFechaHora());
        assertEquals("Solicitud del estudiante", captor.getValue().getTitulo());

        assertEquals(EstadoSolicitud.ATENDIDA, pendiente.getEstado());
        assertSame(funcionario, pendiente.getFuncionario());
        assertSame(referencia, pendiente.getInstancia());
        assertEquals("Te esperamos en la sala 2", pendiente.getRespuesta());
        assertNotNull(pendiente.getFechaResolucion());
    }

    @Test
    @DisplayName("Atender - Una solicitud ya resuelta → 409, no crea ninguna instancia")
    void atender_YaResuelta_409() {
        pendiente.setEstado(EstadoSolicitud.RECHAZADA);
        when(solicitudRepository.findByIdParaActualizar(1)).thenReturn(Optional.of(pendiente));
        AtenderSolicitudInstanciaDTO dto = new AtenderSolicitudInstanciaDTO();
        dto.setIdCategoria(3);
        dto.setFechaHora(LocalDateTime.now().plusDays(1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> solicitudService.atender(1, dto));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(instanciaComunService);
    }

    @Test
    @DisplayName("Atender - Si falla la creación de la instancia, la solicitud no se marca atendida")
    void atender_FallaCrearInstancia_NoResuelve() {
        when(solicitudRepository.findByIdParaActualizar(1)).thenReturn(Optional.of(pendiente));
        when(instanciaComunService.crear(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "categoría inactiva"));
        AtenderSolicitudInstanciaDTO dto = new AtenderSolicitudInstanciaDTO();
        dto.setIdCategoria(3);
        dto.setFechaHora(LocalDateTime.now().plusDays(1));

        assertThrows(ResponseStatusException.class, () -> solicitudService.atender(1, dto));

        assertEquals(EstadoSolicitud.PENDIENTE, pendiente.getEstado());
        verify(solicitudRepository, never()).save(any());
    }

    // ── Rechazar ──────────────────────────────────────────────

    @Test
    @DisplayName("Rechazar - Queda RECHAZADA con la respuesta y el funcionario que la resolvió")
    void rechazar_Exito() {
        autenticarComo("10", "ATENDER_SOLICITUD_INSTANCIA");
        RechazarSolicitudInstanciaDTO dto = new RechazarSolicitudInstanciaDTO();
        dto.setRespuesta("  Lo vemos en la tutoría del jueves  ");
        when(solicitudRepository.findByIdParaActualizar(1)).thenReturn(Optional.of(pendiente));
        when(funcionarioRepository.findById(10)).thenReturn(Optional.of(funcionario));
        when(solicitudRepository.save(pendiente)).thenReturn(pendiente);
        when(solicitudMapper.toDto(pendiente)).thenReturn(responseDTO);

        solicitudService.rechazar(1, dto);

        assertEquals(EstadoSolicitud.RECHAZADA, pendiente.getEstado());
        assertEquals("Lo vemos en la tutoría del jueves", pendiente.getRespuesta());
        assertSame(funcionario, pendiente.getFuncionario());
        assertNull(pendiente.getInstancia());
        verifyNoInteractions(instanciaComunService);
    }

    @Test
    @DisplayName("Rechazar - Solicitud inexistente → 404")
    void rechazar_NoEncontrada_404() {
        when(solicitudRepository.findByIdParaActualizar(99)).thenReturn(Optional.empty());
        RechazarSolicitudInstanciaDTO dto = new RechazarSolicitudInstanciaDTO();
        dto.setRespuesta("No corresponde");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> solicitudService.rechazar(99, dto));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
