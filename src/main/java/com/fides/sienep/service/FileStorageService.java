package com.fides.sienep.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Almacenamiento genérico de archivos subidos (informes adjuntos, foto de
 * perfil de estudiante, etc.) en el filesystem local, bajo app.storage.base-dir.
 * Cada consumidor decide su propia subcarpeta y cómo guarda la referencia
 * (columna única o separada en subcarpeta/nombre) en su entidad.
 */
@Service
public class FileStorageService {

    // Campo separado (no final): Lombok no copia @Value a los parámetros del
    // constructor generado por @RequiredArgsConstructor sin un lombok.config,
    // por lo que esta property se inyecta directamente sobre el campo.
    @Value("${app.storage.base-dir}")
    private String baseDir;

    public String guardar(MultipartFile archivo, String subcarpeta,
                           List<String> extensionesPermitidas, long maxTamanioBytes) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo es obligatorio");
        }

        if (archivo.getSize() > maxTamanioBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo supera el tamaño máximo permitido de " + (maxTamanioBytes / (1024 * 1024)) + "MB");
        }

        String extension = extraerExtension(archivo.getOriginalFilename());
        if (!extensionesPermitidas.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La extensión del archivo no está permitida. Use: "
                            + String.join(", ", extensionesPermitidas).toUpperCase(Locale.ROOT));
        }

        String nombreFisico = UUID.randomUUID() + "." + extension;
        try {
            Path directorio = Path.of(baseDir, subcarpeta);
            Files.createDirectories(directorio);
            archivo.transferTo(directorio.resolve(nombreFisico));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar el archivo en el servidor", e);
        }
        return nombreFisico;
    }

    public Resource cargar(String rutaRelativa) {
        Path ruta = Path.of(baseDir, rutaRelativa);
        if (!Files.exists(ruta)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo físico no se encuentra disponible");
        }
        return new FileSystemResource(ruta);
    }

    public String extraerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe tener una extensión válida");
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public MediaType mediaTypeDeExtension(String extension) {
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
