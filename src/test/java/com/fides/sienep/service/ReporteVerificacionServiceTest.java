package com.fides.sienep.service;

import com.fides.sienep.dto.ReporteValidacionResponseDTO;
import com.fides.sienep.model.ReporteGenerado;
import com.fides.sienep.repository.ReporteGeneradoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteVerificacionServiceTest {

    private static final String IP = "190.64.20.10";

    @Mock private ReporteGeneradoRepository reporteGeneradoRepository;
    @Mock private ReporteValidacionRateLimiter reporteValidacionRateLimiter;

    @InjectMocks
    private ReporteVerificacionService reporteVerificacionService;

    @BeforeEach
    void setUp() {
        lenient().when(reporteValidacionRateLimiter.registrarIntento(any())).thenReturn(true);
    }

    @Test
    @DisplayName("registrarGeneracion calcula el hash SHA-256 del PDF y guarda el registro")
    void registrarGeneracion_guardaElRegistroConElHashCorrecto() {
        byte[] pdfBytes = "contenido-pdf-simulado".getBytes(StandardCharsets.UTF_8);

        reporteVerificacionService.registrarGeneracion(pdfBytes);

        ArgumentCaptor<ReporteGenerado> captor = ArgumentCaptor.forClass(ReporteGenerado.class);
        verify(reporteGeneradoRepository).save(captor.capture());

        assertEquals(hashEsperado(pdfBytes), captor.getValue().getHashSha256());
    }

    @Test
    @DisplayName("validar devuelve autentico=true cuando el hash del archivo subido coincide con un registro existente")
    void validar_archivoCoincide_devuelveAutentico() {
        byte[] contenido = "contenido-pdf-simulado".getBytes(StandardCharsets.UTF_8);
        MultipartFile archivo = new MockMultipartFile("archivo", "reporte.pdf", "application/pdf", contenido);

        ReporteGenerado registro = new ReporteGenerado(hashEsperado(contenido));
        when(reporteGeneradoRepository.findByHashSha256(hashEsperado(contenido)))
                .thenReturn(Optional.of(registro));

        ReporteValidacionResponseDTO resultado = reporteVerificacionService.validar(archivo, IP);

        assertTrue(resultado.isAutentico());
    }

    @Test
    @DisplayName("validar devuelve autentico=false cuando el hash del archivo subido no está registrado")
    void validar_archivoNoCoincide_devuelveNoAutentico() {
        byte[] contenido = "archivo-alterado-o-falso".getBytes(StandardCharsets.UTF_8);
        MultipartFile archivo = new MockMultipartFile("archivo", "reporte.pdf", "application/pdf", contenido);

        when(reporteGeneradoRepository.findByHashSha256(any())).thenReturn(Optional.empty());

        ReporteValidacionResponseDTO resultado = reporteVerificacionService.validar(archivo, IP);

        assertFalse(resultado.isAutentico());
    }

    @Test
    @DisplayName("validar rechaza un archivo vacío con 400")
    void validar_archivoVacio_lanzaExcepcion() {
        MultipartFile archivoVacio = new MockMultipartFile("archivo", "vacio.pdf", "application/pdf", new byte[0]);

        assertThrows(ResponseStatusException.class, () -> reporteVerificacionService.validar(archivoVacio, IP));
        verifyNoInteractions(reporteGeneradoRepository);
    }

    @Test
    @DisplayName("validar rechaza con 429 cuando se supera el límite de intentos por IP")
    void validar_limiteDeIpExcedido_lanzaExcepcion() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "reporte.pdf", "application/pdf", "contenido".getBytes(StandardCharsets.UTF_8));
        when(reporteValidacionRateLimiter.registrarIntento(IP)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> reporteVerificacionService.validar(archivo, IP));
        verifyNoInteractions(reporteGeneradoRepository);
    }

    private String hashEsperado(byte[] datos) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(datos));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
