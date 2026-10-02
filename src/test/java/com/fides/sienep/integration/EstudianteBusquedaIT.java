package com.fides.sienep.integration;

import com.fides.sienep.dto.EstudianteResumenDTO;
import com.fides.sienep.model.Carrera;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Grupo;
import com.fides.sienep.model.Itr;
import com.fides.sienep.model.ItrCarrera;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.repository.CarreraRepository;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.GrupoRepository;
import com.fides.sienep.repository.ItrCarreraRepository;
import com.fides.sienep.repository.ItrRepository;
import com.fides.sienep.repository.RolRepository;
import com.fides.sienep.service.EstudianteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Test de integración real contra Postgres (requiere `docker compose up -d` con el schema ya aplicado). Igual
// que ComentarioConfidencialInstanciaIT, el sufijo "IT" lo deja fuera de `./mvnw test`; se corre con:
//
//   docker compose up -d
//   ./mvnw test -Dtest=EstudianteBusquedaIT -Dspring.profiles.active=integration
//
// Motivo: RF08 pide combinar los filtros del listado de estudiantes (texto, grupo, carrera, ITR, estado). Con
// Mockito no se puede verificar qué devuelve una Specification, solo que se llamó; acá se ejecuta la consulta
// de verdad. @Transactional: todo lo creado se deshace al terminar cada test.
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class EstudianteBusquedaIT {

    // Nombre que no puede aparecer en otros datos de la base, para que los resultados sean solo los de este test.
    private static final String MARCA = "Zanahoria";

    @Autowired private EstudianteService estudianteService;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private ItrRepository itrRepository;
    @Autowired private CarreraRepository carreraRepository;
    @Autowired private ItrCarreraRepository itrCarreraRepository;
    @Autowired private GrupoRepository grupoRepository;

    private Itr itrSur;
    private Carrera carreraA;
    private Grupo grupoA1;
    private Grupo grupoB1;

    @BeforeEach
    void setUp() {
        Itr itrNorte = itrRepository.save(new Itr("ITR IT Norte"));
        itrSur = itrRepository.save(new Itr("ITR IT Sur"));
        carreraA = carreraRepository.save(new Carrera("Carrera IT A"));
        Carrera carreraB = carreraRepository.save(new Carrera("Carrera IT B"));
        itrCarreraRepository.save(new ItrCarrera(itrNorte, carreraA));
        itrCarreraRepository.save(new ItrCarrera(itrSur, carreraB));

        grupoA1 = grupo(carreraA, "A1");
        Grupo grupoA2 = grupo(carreraA, "A2");
        grupoB1 = grupo(carreraB, "B1");

        // Ana está en DOS grupos de la misma carrera: no debe salir duplicada al filtrar por carrera.
        estudiante(MARCA, "Ana", "90000101", EstadoLogico.ACTIVO, grupoA1, grupoA2);
        estudiante(MARCA, "Bruno", "90000102", EstadoLogico.ACTIVO, grupoB1);
        estudiante("Berenjena", "Carla", "90000103", EstadoLogico.ACTIVO, grupoA1);
        estudiante(MARCA, "Dario", "90000104", EstadoLogico.INACTIVO, grupoA1);

        // Escribe los datos ahora, sin usuario autenticado: si se escribieran recién en la búsqueda, el trigger de
        // auditoría tomaría el usuario simulado de autenticarCon() como autor, y ese id no existe en la base.
        estudianteRepository.flush();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Grupo grupo(Carrera carrera, String nombre) {
        Grupo g = new Grupo();
        g.setCarrera(carrera);
        g.setNomGrupo(nombre);
        g.setGeneracion(2024);
        g.setEstado(EstadoLogico.ACTIVO);
        return grupoRepository.save(g);
    }

    private void estudiante(String nombre, String apellido, String documento, EstadoLogico estado, Grupo... grupos) {
        Estudiante e = new Estudiante(nombre, apellido, documento, PaisDocumento.ARG, "hash",
                documento + "@it.utec.edu.uy", null, null, null, null, null, null,
                LocalDate.of(2000, 1, 1), null, null, null, null);
        e.setEstado(estado);
        e.setRol(rolRepository.findByNombre("ESTUDIANTE").orElseThrow());
        e.setGrupos(new java.util.ArrayList<>(Arrays.asList(grupos)));
        estudianteRepository.save(e);
    }

    private void autenticarCon(String... permisos) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "1", null, Arrays.stream(permisos).map(SimpleGrantedAuthority::new).toList()));
    }

    private List<String> apellidos(Page<EstudianteResumenDTO> pagina) {
        return pagina.getContent().stream().map(EstudianteResumenDTO::getApellido).sorted().toList();
    }

    private Page<EstudianteResumenDTO> listar(String texto, Integer grupo, Integer carrera, Integer itr,
                                              EstadoLogico estado) {
        return estudianteService.listar(texto, grupo, carrera, itr, estado, PageRequest.of(0, 50));
    }

    @Test
    @DisplayName("Texto + carrera + estado: aplica los tres (antes se ignoraba la carrera)")
    void textoCarreraYEstado_SeCombinan() {
        autenticarCon("VER_ESTUDIANTE_RESUMIDO", "VER_ESTUDIANTE", "BUSCAR_ESTUDIANTE");

        Page<EstudianteResumenDTO> resultado = listar(MARCA, null, carreraA.getIdCarrera(), null, EstadoLogico.ACTIVO);

        assertEquals(List.of("Ana"), apellidos(resultado));
    }

    @Test
    @DisplayName("Carrera sola: un estudiante en dos grupos de la misma carrera sale una sola vez, y el total es exacto")
    void carrera_SinDuplicados() {
        autenticarCon("VER_ESTUDIANTE_RESUMIDO", "VER_ESTUDIANTE", "BUSCAR_ESTUDIANTE");

        Page<EstudianteResumenDTO> resultado = listar(null, null, carreraA.getIdCarrera(), null, null);

        assertEquals(List.of("Ana", "Carla", "Dario"), apellidos(resultado));
        assertEquals(3, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Grupo + estado")
    void grupoYEstado_SeCombinan() {
        autenticarCon("VER_ESTUDIANTE_RESUMIDO", "VER_ESTUDIANTE", "BUSCAR_ESTUDIANTE");

        Page<EstudianteResumenDTO> resultado = listar(null, grupoA1.getIdGrupo(), null, null, EstadoLogico.ACTIVO);

        assertEquals(List.of("Ana", "Carla"), apellidos(resultado));
    }

    @Test
    @DisplayName("ITR + texto")
    void itrYTexto_SeCombinan() {
        autenticarCon("VER_ESTUDIANTE_RESUMIDO", "VER_ESTUDIANTE", "BUSCAR_ESTUDIANTE");

        Page<EstudianteResumenDTO> resultado = listar(MARCA, null, null, itrSur.getIdItr(), null);

        assertEquals(List.of("Bruno"), apellidos(resultado));
    }

    @Test
    @DisplayName("Filtros contradictorios (carrera A + grupo de la carrera B): ningún resultado")
    void filtrosContradictorios_SinResultados() {
        autenticarCon("VER_ESTUDIANTE_RESUMIDO", "VER_ESTUDIANTE", "BUSCAR_ESTUDIANTE");

        Page<EstudianteResumenDTO> resultado = listar(null, grupoB1.getIdGrupo(), carreraA.getIdCarrera(), null, null);

        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Sin VER_ESTUDIANTE: aunque pida INACTIVO, solo ve activos (regla existente, sigue combinándose)")
    void sinVerEstudiante_SoloActivos() {
        autenticarCon("VER_ESTUDIANTE_RESUMIDO", "BUSCAR_ESTUDIANTE");

        Page<EstudianteResumenDTO> resultado = listar(null, null, carreraA.getIdCarrera(), null, EstadoLogico.INACTIVO);

        assertEquals(List.of("Ana", "Carla"), apellidos(resultado));
    }
}
