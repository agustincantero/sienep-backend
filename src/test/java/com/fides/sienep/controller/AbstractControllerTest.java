package com.fides.sienep.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fides.sienep.security.CustomUsuarioDetailsService;
import com.fides.sienep.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Base común para los *ControllerTest de "contrato HTTP" (P1 de la auditoría):
// fijan @Valid → 400, 404, forma del JSON — con el service mockeado (a
// diferencia de los tests de comentarios confidenciales, que son P0 de
// autorización real y por eso usan @SpringBootTest con el service sin mockear).
//
// Cualquier @WebMvcTest en este proyecto necesita estos tres mocks porque:
//   - JwtRequestFilter (real, se carga en todo @WebMvcTest por ser un Filter)
//     requiere CustomUsuarioDetailsService y JwtTokenUtil en su constructor.
//   - SienepApplication (la @SpringBootConfiguration base de todo @WebMvcTest)
//     tiene @EnableCaching a nivel de clase, que exige un CacheManager en el
//     contexto sin importar qué controller se esté testeando.
// @WithMockUser deja la request autenticada sin depender del filtro JWT real
// (eso ya se cubre aparte en JwtRequestFilterTest y en los tests de comentarios
// confidenciales) — acá el objetivo es el contrato de entrada/salida, no la
// autenticación ni la autorización.
@ActiveProfiles("test")
@WithMockUser
public abstract class AbstractControllerTest {

    @Autowired protected MockMvc mockMvc;

    @MockitoBean protected CustomUsuarioDetailsService customUsuarioDetailsService;
    @MockitoBean protected JwtTokenUtil jwtTokenUtil;
    @MockitoBean protected CacheManager cacheManager;

    // findAndRegisterModules() registra jackson-datatype-jsr310 (LocalDate/
    // LocalDateTime) si está en el classpath de test — un ObjectMapper "pelado"
    // no lo hace solo y falla al (de)serializar esos tipos, muy comunes en los
    // DTOs de este proyecto (fechaNacimiento, createdAt, etc.).
    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
}
