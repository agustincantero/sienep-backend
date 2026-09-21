package com.fides.sienep.service;

import com.fides.sienep.dto.ReporteValidacionResponseDTO;
import com.fides.sienep.model.ReporteGenerado;
import com.fides.sienep.repository.ReporteGeneradoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReporteVerificacionService {

    private final ReporteGeneradoRepository reporteGeneradoRepository;
    private final ReporteValidacionRateLimiter reporteValidacionRateLimiter;

    @Transactional
    public void registrarGeneracion(byte[] pdfBytes) {
        String hash = calcularHashSha256(pdfBytes);
        reporteGeneradoRepository.save(new ReporteGenerado(hash));
    }

    @Transactional(readOnly = true)
    public ReporteValidacionResponseDTO validar(MultipartFile archivo, String ip) {
        if (!reporteValidacionRateLimiter.registrarIntento(ip)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiadas validaciones desde esta IP. Intenta nuevamente más tarde.");
        }

        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe adjuntar un archivo PDF");
        }

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo adjuntado", e);
        }

        String hash = calcularHashSha256(contenido);
        Optional<ReporteGenerado> registro = reporteGeneradoRepository.findByHashSha256(hash);

        if (registro.isEmpty()) {
            return ReporteValidacionResponseDTO.builder()
                    .autentico(false)
                    .build();
        }

        ReporteGenerado r = registro.get();
        return ReporteValidacionResponseDTO.builder()
                .autentico(true)
                .fechaGeneracion(r.getCreatedAt())
                .build();
    }

    private String calcularHashSha256(byte[] datos) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(datos);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("El algoritmo SHA-256 no está disponible en esta JVM", e);
        }
    }
}
