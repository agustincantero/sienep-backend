package com.fides.sienep.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path baseDir;

    private static final List<String> EXTENSIONES = List.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_TAMANIO = 5L * 1024 * 1024;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "baseDir", baseDir.toString());
    }

    @Test
    @DisplayName("Guardar - Éxito, crea el subdirectorio y devuelve un nombre físico con la extensión correcta")
    void guardar_Exito() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", "application/pdf", "contenido".getBytes());

        String nombreFisico = fileStorageService.guardar(archivo, "informes/1", EXTENSIONES, MAX_TAMANIO);

        assertTrue(nombreFisico.endsWith(".pdf"));
        assertTrue(Files.exists(baseDir.resolve("informes/1").resolve(nombreFisico)));
    }

    @Test
    @DisplayName("Guardar - Éxito con extensión en mayúsculas, normaliza a minúsculas")
    void guardar_Exito_ExtensionMayuscula() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "imagen.JPG", "image/jpeg", "contenido".getBytes());

        String nombreFisico = fileStorageService.guardar(archivo, "fotos/1", EXTENSIONES, MAX_TAMANIO);

        assertTrue(nombreFisico.endsWith(".jpg"));
    }

    @Test
    @DisplayName("Guardar - Archivo vacío (400)")
    void guardar_LanzaExcepcion_ArchivoVacio() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", "application/pdf", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fileStorageService.guardar(archivo, "informes/1", EXTENSIONES, MAX_TAMANIO));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Guardar - Extensión no permitida (400)")
    void guardar_LanzaExcepcion_ExtensionNoPermitida() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.exe", "application/octet-stream", "contenido".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fileStorageService.guardar(archivo, "informes/1", EXTENSIONES, MAX_TAMANIO));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Guardar - Supera el tamaño máximo (400)")
    void guardar_LanzaExcepcion_TamanioExcedido() {
        MultipartFile archivo = new MockMultipartFile(
                "archivo", "informe.pdf", "application/pdf", new byte[(int) MAX_TAMANIO + 1]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fileStorageService.guardar(archivo, "informes/1", EXTENSIONES, MAX_TAMANIO));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Cargar - Éxito cuando el archivo físico existe")
    void cargar_Exito() throws Exception {
        Path directorio = baseDir.resolve("informes/1");
        Files.createDirectories(directorio);
        Files.write(directorio.resolve("archivo.pdf"), "contenido".getBytes());

        Resource recurso = fileStorageService.cargar("informes/1/archivo.pdf");

        assertTrue(recurso.exists());
    }

    @Test
    @DisplayName("Cargar - Archivo no existe (404)")
    void cargar_LanzaExcepcion_ArchivoNoExiste() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fileStorageService.cargar("informes/1/no-existe.pdf"));

        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Extraer extensión - Sin extensión (400)")
    void extraerExtension_LanzaExcepcion_SinExtension() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> fileStorageService.extraerExtension("archivo-sin-extension"));

        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("MediaType de extensión - Casos conocidos y desconocido")
    void mediaTypeDeExtension_Casos() {
        assertEquals(MediaType.APPLICATION_PDF, fileStorageService.mediaTypeDeExtension("pdf"));
        assertEquals(MediaType.IMAGE_JPEG, fileStorageService.mediaTypeDeExtension("jpg"));
        assertEquals(MediaType.IMAGE_JPEG, fileStorageService.mediaTypeDeExtension("jpeg"));
        assertEquals(MediaType.IMAGE_PNG, fileStorageService.mediaTypeDeExtension("png"));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, fileStorageService.mediaTypeDeExtension("exe"));
    }
}
