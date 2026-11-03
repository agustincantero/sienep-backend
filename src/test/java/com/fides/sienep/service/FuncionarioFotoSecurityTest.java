package com.fides.sienep.service;

import com.fides.sienep.mapper.FuncionarioMapper;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.RolRepository;
import com.fides.sienep.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

// Verifica las reglas de @PreAuthorize de la foto de funcionario con @EnableMethodSecurity real (en
// FuncionarioServiceTest, con Mockito puro, no se evalúan). Mismo esquema que
// InformeAdjuntoServiceSecurityTest: "pasa la seguridad" = la llamada llega al cuerpo del método y el
// repositorio mockeado no encuentra al funcionario (404), en vez de AccessDenied.
@SpringJUnitConfig(FuncionarioFotoSecurityTest.Config.class)
class FuncionarioFotoSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        FuncionarioService funcionarioService() {
            return new FuncionarioService(
                    mock(FuncionarioRepository.class), mock(UsuarioRepository.class), mock(RolRepository.class), mock(PasswordEncoder.class),
                    mock(FuncionarioMapper.class), mock(EmailService.class), mock(PasswordGeneratorService.class),
                    mock(FileStorageService.class));
        }
    }

    @Autowired private FuncionarioService funcionarioService;

    private final MockMultipartFile foto = new MockMultipartFile("foto", "perfil.png", "image/png", new byte[]{1});

    private void assertPasaLaSeguridad(Runnable llamada) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, llamada::run);
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @WithMockUser(username = "3")
    @DisplayName("Subir foto - El propio funcionario puede subir la suya sin EDITAR_FUNCIONARIO")
    void subirFoto_Propio_PasaLaSeguridad() {
        assertPasaLaSeguridad(() -> funcionarioService.subirFoto(3, foto));
    }

    @Test
    @WithMockUser(username = "3")
    @DisplayName("Subir foto - Sin EDITAR_FUNCIONARIO no puede subir la de otro")
    void subirFoto_Ajeno_AccessDenied() {
        assertThrows(AccessDeniedException.class, () -> funcionarioService.subirFoto(4, foto));
    }

    @Test
    @WithMockUser(username = "3", authorities = "EDITAR_FUNCIONARIO")
    @DisplayName("Subir foto - Con EDITAR_FUNCIONARIO puede subir la de otro")
    void subirFoto_AjenoConPermiso_PasaLaSeguridad() {
        assertPasaLaSeguridad(() -> funcionarioService.subirFoto(4, foto));
    }

    @Test
    @WithMockUser(username = "3")
    @DisplayName("Ver foto - El propio funcionario puede ver la suya sin VER_FUNCIONARIOS")
    void obtenerFoto_Propia_PasaLaSeguridad() {
        assertPasaLaSeguridad(() -> funcionarioService.obtenerFoto(3));
    }

    @Test
    @WithMockUser(username = "3")
    @DisplayName("Ver foto - Sin VER_FUNCIONARIOS no puede ver la de otro")
    void obtenerFoto_Ajena_AccessDenied() {
        assertThrows(AccessDeniedException.class, () -> funcionarioService.obtenerFoto(4));
    }

    @Test
    @WithMockUser(username = "3", authorities = "VER_FUNCIONARIOS")
    @DisplayName("Ver foto - Con VER_FUNCIONARIOS puede ver la de otro")
    void obtenerFoto_AjenaConPermiso_PasaLaSeguridad() {
        assertPasaLaSeguridad(() -> funcionarioService.obtenerFoto(4));
    }
}
