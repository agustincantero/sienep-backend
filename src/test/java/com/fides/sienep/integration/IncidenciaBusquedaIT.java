package com.fides.sienep.integration;

import com.fides.sienep.dto.IncidenciaResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Incidencia;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.IncidenciaRepository;
import com.fides.sienep.repository.RolRepository;
import com.fides.sienep.service.IncidenciaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Test de integración real contra Postgres (requiere `docker compose up -d` con el schema ya aplicado). Igual que
// InstanciaBusquedaIT, el sufijo "IT" lo deja fuera de `./mvnw test`; se corre con:
//
//   docker compose up -d
//   ./mvnw test -Dtest=IncidenciaBusquedaIT -Dspring.profiles.active=integration
//
// Motivo: GET /incidencias combina los filtros estudiante, funcionario y estado. Antes el controller elegía una
// única query según el primer filtro presente y descartaba el resto sin avisar (estudiante + funcionario devolvía
// todas las incidencias del estudiante). @Transactional: todo lo creado se deshace al terminar cada test.
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class IncidenciaBusquedaIT {

    @Autowired private IncidenciaService incidenciaService;
    @Autowired private IncidenciaRepository incidenciaRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private RolRepository rolRepository;

    private Estudiante estudiante1;
    private Estudiante estudiante2;
    private Funcionario funcionarioA;
    private Funcionario funcionarioB;

    @BeforeEach
    void setUp() {
        Rol rolAdmin = rolRepository.findByNombre("ADMINISTRADOR").orElseThrow();

        estudiante1 = estudiante("Uno", "90000311");
        estudiante2 = estudiante("Dos", "90000312");
        funcionarioA = funcionario("A", "90000321", rolAdmin);
        funcionarioB = funcionario("B", "90000322", rolAdmin);

        // El estudiante 1 tiene 4 incidencias (3 activas, 1 inactiva); el 2, una activa.
        incidencia("IT-1", estudiante1, funcionarioA, EstadoLogico.ACTIVO);
        incidencia("IT-2", estudiante1, funcionarioB, EstadoLogico.ACTIVO);
        incidencia("IT-3", estudiante1, funcionarioA, EstadoLogico.ACTIVO);
        incidencia("IT-4", estudiante2, funcionarioA, EstadoLogico.ACTIVO);
        incidencia("IT-5", estudiante1, funcionarioA, EstadoLogico.INACTIVO);

        // Escribe los datos ahora, sin usuario autenticado: si se escribieran recién en el listado, el trigger de
        // auditoría tomaría el usuario simulado de autenticar() como autor, y ese id no existe en la base.
        incidenciaRepository.flush();
        autenticar();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Estudiante estudiante(String apellido, String documento) {
        Estudiante e = new Estudiante("IT", apellido, documento, PaisDocumento.ARG, "hash",
                documento + "@it.utec.edu.uy", null, null, null, null, null, null,
                LocalDate.of(2000, 1, 1), null, null, null, null);
        e.setEstado(EstadoLogico.ACTIVO);
        e.setRol(rolRepository.findByNombre("ESTUDIANTE").orElseThrow());
        return estudianteRepository.save(e);
    }

    private Funcionario funcionario(String apellido, String documento, Rol rol) {
        Funcionario f = new Funcionario("IT", apellido, documento, PaisDocumento.URY, "hash",
                documento + "@it.utec.edu.uy", rol);
        f.setEstado(EstadoLogico.ACTIVO);
        return funcionarioRepository.save(f);
    }

    private void incidencia(String titulo, Estudiante estudiante, Funcionario funcionario, EstadoLogico estado) {
        Incidencia i = new Incidencia(titulo, LocalDateTime.now().minusHours(1), estudiante, funcionario,
                "Presencial", "Aula IT");
        i.setEstado(estado);
        incidenciaRepository.save(i);
    }

    private void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "1", null, List.of(new SimpleGrantedAuthority("VER_INCIDENCIAS"))));
    }

    private Page<IncidenciaResponseDTO> listar(Estudiante estudiante, Funcionario funcionario, EstadoLogico estado) {
        return listar(estudiante, funcionario, estado, PageRequest.of(0, 50));
    }

    private Page<IncidenciaResponseDTO> listar(Estudiante estudiante, Funcionario funcionario, EstadoLogico estado,
                                               PageRequest pageable) {
        return incidenciaService.listar(
                estudiante != null ? estudiante.getIdUsuario() : null,
                funcionario != null ? funcionario.getIdUsuario() : null,
                estado != null ? List.of(estado) : null, pageable);
    }

    private List<String> titulos(Page<IncidenciaResponseDTO> pagina) {
        return pagina.getContent().stream().map(IncidenciaResponseDTO::getTitulo).sorted().toList();
    }

    @Test
    @DisplayName("Estudiante + funcionario: aplica los dos (antes se ignoraba el funcionario)")
    void estudianteYFuncionario_SeCombinan() {
        Page<IncidenciaResponseDTO> resultado = listar(estudiante1, funcionarioA, null);

        assertEquals(List.of("IT-1", "IT-3"), titulos(resultado));
    }

    @Test
    @DisplayName("Solo funcionario: trae las activas de ese funcionario de todos los estudiantes")
    void soloFuncionario() {
        Page<IncidenciaResponseDTO> resultado = listar(null, funcionarioA, null);

        assertEquals(List.of("IT-1", "IT-3", "IT-4"), titulos(resultado));
    }

    @Test
    @DisplayName("Sin estado devuelve solo ACTIVAS; con INACTIVO, solo las inactivas, y se combina con los otros filtros")
    void estado_SeCombinaConElResto() {
        assertEquals(List.of("IT-1", "IT-2", "IT-3"), titulos(listar(estudiante1, null, null)));
        assertEquals(List.of("IT-5"), titulos(listar(estudiante1, null, EstadoLogico.INACTIVO)));
        assertEquals(List.of("IT-5"), titulos(listar(estudiante1, funcionarioA, EstadoLogico.INACTIVO)));
        assertEquals(0, listar(estudiante1, funcionarioB, EstadoLogico.INACTIVO).getTotalElements());
    }

    @Test
    @DisplayName("Filtros contradictorios (estudiante 2 + funcionario B): ningún resultado")
    void filtrosContradictorios_SinResultados() {
        Page<IncidenciaResponseDTO> resultado = listar(estudiante2, funcionarioB, null);

        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    @DisplayName("La respuesta incluye el documento del estudiante y del funcionario")
    void respuesta_IncluyeLosDocumentos() {
        Page<IncidenciaResponseDTO> resultado = listar(estudiante2, funcionarioA, null);

        assertEquals(1, resultado.getNumberOfElements());
        assertEquals("90000312", resultado.getContent().get(0).getDocumentoEstudiante());
        assertEquals("90000321", resultado.getContent().get(0).getDocumentoFuncionario());
    }

    @Test
    @DisplayName("Con el orden por defecto del controller (fecha y código descendentes) las páginas no repiten ni saltean filas")
    void ordenPorDefecto_PaginasEstables() {
        Sort orden = Sort.by(Sort.Direction.DESC, "fechaHora", "codInstancia");

        Page<IncidenciaResponseDTO> pagina1 = listar(estudiante1, null, null, PageRequest.of(0, 2, orden));
        Page<IncidenciaResponseDTO> pagina2 = listar(estudiante1, null, null, PageRequest.of(1, 2, orden));

        // Se crearon en orden IT-1, IT-2, IT-3 (fecha y código crecientes): de la más nueva a la más vieja.
        List<String> recorridas = new ArrayList<>();
        pagina1.getContent().forEach(i -> recorridas.add(i.getTitulo()));
        pagina2.getContent().forEach(i -> recorridas.add(i.getTitulo()));
        assertEquals(List.of("IT-3", "IT-2", "IT-1"), recorridas);
    }

    @Test
    @DisplayName("Con filtros combinados la paginación es exacta: el total cuenta todas, la página trae solo su parte")
    void paginacion_ConFiltrosCombinados() {
        Page<IncidenciaResponseDTO> pagina1 = listar(estudiante1, null, null, PageRequest.of(0, 2));
        Page<IncidenciaResponseDTO> pagina2 = listar(estudiante1, null, null, PageRequest.of(1, 2));

        assertEquals(3, pagina1.getTotalElements());
        assertEquals(2, pagina1.getNumberOfElements());
        assertEquals(1, pagina2.getNumberOfElements());
    }
}
