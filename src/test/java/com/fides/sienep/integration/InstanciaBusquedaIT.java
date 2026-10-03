package com.fides.sienep.integration;

import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.CategoriaInstanciaRepository;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.repository.RolRepository;
import com.fides.sienep.service.InstanciaComunService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Test de integración real contra Postgres (requiere `docker compose up -d` con el schema ya aplicado). Igual que
// EstudianteBusquedaIT, el sufijo "IT" lo deja fuera de `./mvnw test`; se corre con:
//
//   docker compose up -d
//   ./mvnw test -Dtest=InstanciaBusquedaIT -Dspring.profiles.active=integration
//
// Motivo: GET /instancias combina los filtros estudiante, funcionario, categoría y estado. Antes el controller
// elegía una única query según el primer filtro presente y descartaba el resto sin avisar (estudiante + categoría
// devolvía todas las instancias del estudiante). Con Mockito no se puede verificar qué devuelve una Specification,
// solo que se llamó; acá se ejecuta la consulta de verdad. @Transactional: todo lo creado se deshace al terminar
// cada test.
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class InstanciaBusquedaIT {

    @Autowired private InstanciaComunService instanciaComunService;
    @Autowired private InstanciaComunRepository instanciaComunRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private CategoriaInstanciaRepository categoriaInstanciaRepository;
    @Autowired private RolRepository rolRepository;

    private Estudiante estudiante1;
    private Estudiante estudiante2;
    private Funcionario funcionarioA;
    private Funcionario funcionarioB;
    private CategoriaInstancia categoriaX;
    private CategoriaInstancia categoriaY;

    @BeforeEach
    void setUp() {
        Rol rolAdmin = rolRepository.findByNombre("ADMINISTRADOR").orElseThrow();
        List<CategoriaInstancia> categorias = categoriaInstanciaRepository.findAll();
        if (categorias.size() < 2) {
            throw new IllegalStateException("Hacen falta 2 categorías de instancia — ¿se corrió proyecto_schema.sql?");
        }
        categoriaX = categorias.get(0);
        categoriaY = categorias.get(1);

        estudiante1 = estudiante("Uno", "90000211");
        estudiante2 = estudiante("Dos", "90000212");
        funcionarioA = funcionario("A", "90000221", rolAdmin);
        funcionarioB = funcionario("B", "90000222", rolAdmin);

        // El estudiante 1 tiene 4 instancias (3 activas, 1 inactiva); el 2, una activa.
        instancia("IT-1", estudiante1, funcionarioA, categoriaX, EstadoLogico.ACTIVO);
        instancia("IT-2", estudiante1, funcionarioA, categoriaY, EstadoLogico.ACTIVO);
        instancia("IT-3", estudiante1, funcionarioB, categoriaX, EstadoLogico.ACTIVO);
        instancia("IT-4", estudiante2, funcionarioA, categoriaX, EstadoLogico.ACTIVO);
        instancia("IT-5", estudiante1, funcionarioA, categoriaX, EstadoLogico.INACTIVO);

        // Escribe los datos ahora, sin usuario autenticado: si se escribieran recién en el listado, el trigger de
        // auditoría tomaría el usuario simulado de autenticar() como autor, y ese id no existe en la base.
        instanciaComunRepository.flush();
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

    private void instancia(String titulo, Estudiante estudiante, Funcionario funcionario,
                           CategoriaInstancia categoria, EstadoLogico estado) {
        InstanciaComun i = new InstanciaComun(titulo, LocalDateTime.now().plusDays(1), estudiante, funcionario,
                "Presencial", categoria);
        i.setEstado(estado);
        instanciaComunRepository.save(i);
    }

    private void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "1", null, List.of(new SimpleGrantedAuthority("VER_INSTANCIAS"))));
    }

    private Page<InstanciaComunResponseDTO> listar(Estudiante estudiante, Funcionario funcionario,
                                                   CategoriaInstancia categoria, EstadoLogico estado) {
        return listar(estudiante, funcionario, categoria, estado, PageRequest.of(0, 50));
    }

    private Page<InstanciaComunResponseDTO> listar(Estudiante estudiante, Funcionario funcionario,
                                                   CategoriaInstancia categoria, EstadoLogico estado,
                                                   PageRequest pageable) {
        return instanciaComunService.listar(
                estudiante != null ? estudiante.getIdUsuario() : null,
                funcionario != null ? funcionario.getIdUsuario() : null,
                categoria != null ? categoria.getIdCategoria() : null,
                estado, pageable);
    }

    private List<String> titulos(Page<InstanciaComunResponseDTO> pagina) {
        return pagina.getContent().stream().map(InstanciaComunResponseDTO::getTitulo).sorted().toList();
    }

    @Test
    @DisplayName("Estudiante + categoría: aplica las dos (antes se ignoraba la categoría)")
    void estudianteYCategoria_SeCombinan() {
        Page<InstanciaComunResponseDTO> resultado = listar(estudiante1, null, categoriaX, null);

        assertEquals(List.of("IT-1", "IT-3"), titulos(resultado));
    }

    @Test
    @DisplayName("Estudiante + funcionario")
    void estudianteYFuncionario_SeCombinan() {
        Page<InstanciaComunResponseDTO> resultado = listar(estudiante1, funcionarioA, null, null);

        assertEquals(List.of("IT-1", "IT-2"), titulos(resultado));
    }

    @Test
    @DisplayName("Funcionario + categoría (antes solo se aplicaba el funcionario)")
    void funcionarioYCategoria_SeCombinan() {
        Page<InstanciaComunResponseDTO> resultado = listar(null, funcionarioA, categoriaX, null);

        // El funcionario A tiene IT-1, IT-2, IT-4 activas; la categoría deja IT-1 y IT-4.
        assertEquals(List.of("IT-1", "IT-4"), titulos(resultado));
    }

    @Test
    @DisplayName("Estudiante + funcionario + categoría: los tres a la vez")
    void losTresFiltros_SeCombinan() {
        Page<InstanciaComunResponseDTO> resultado = listar(estudiante1, funcionarioA, categoriaX, null);

        assertEquals(List.of("IT-1"), titulos(resultado));
    }

    @Test
    @DisplayName("Sin estado devuelve solo ACTIVAS; con INACTIVO, solo las inactivas, y se combina con el estudiante")
    void estado_SeCombinaConElResto() {
        assertEquals(List.of("IT-1", "IT-2", "IT-3"), titulos(listar(estudiante1, null, null, null)));
        assertEquals(List.of("IT-5"), titulos(listar(estudiante1, null, null, EstadoLogico.INACTIVO)));
    }

    @Test
    @DisplayName("Filtros contradictorios (estudiante 2 + funcionario B): ningún resultado")
    void filtrosContradictorios_SinResultados() {
        Page<InstanciaComunResponseDTO> resultado = listar(estudiante2, funcionarioB, null, null);

        assertEquals(0, resultado.getTotalElements());
    }

    @Test
    @DisplayName("Con filtros combinados la paginación es exacta: el total cuenta todas, la página trae solo su parte")
    void paginacion_ConFiltrosCombinados() {
        Page<InstanciaComunResponseDTO> pagina1 = listar(estudiante1, null, null, null, PageRequest.of(0, 2));
        Page<InstanciaComunResponseDTO> pagina2 = listar(estudiante1, null, null, null, PageRequest.of(1, 2));

        assertEquals(3, pagina1.getTotalElements());
        assertEquals(2, pagina1.getNumberOfElements());
        assertEquals(1, pagina2.getNumberOfElements());
    }
}
