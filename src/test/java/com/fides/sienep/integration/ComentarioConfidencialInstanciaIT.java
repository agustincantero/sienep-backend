package com.fides.sienep.integration;

import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.ComentarioConfidencialInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.CategoriaInstanciaRepository;
import com.fides.sienep.repository.ComentarioConfidencialInstanciaRepository;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.repository.RolRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Test de integración real contra Postgres (requiere `docker compose up -d`
// con el schema ya aplicado). NO corre con `./mvnw test`: el sufijo "IT" (en vez de
// "Test") lo excluye a propósito de los patrones que Surefire toma por defecto, para
// no romper la garantía documentada en CLAUDE.md de que la suite normal no necesita
// Docker. Se ejecuta explícitamente con:
//
//   docker compose up -d
//   JAVA_HOME=<jdk21+> ./mvnw test -Dtest=ComentarioConfidencialInstanciaIT -Dspring.profiles.active=integration
//
// Motivo del test: ComentarioConfidencialInstancia usa @EmbeddedId + @MapsId (clave
// compuesta id_com_confidencial + cod_instancia, ver el modelo) — una zona de JPA/
// Hibernate más propensa a bugs sutiles de mapeo (ID no generado, mapeo de la FK
// compartida roto, etc.) que los tests unitarios con Mockito no pueden detectar
// porque nunca llegan a golpear un EntityManager real.
@SpringBootTest
@ActiveProfiles("integration")
class ComentarioConfidencialInstanciaIT {

    @Autowired private RolRepository rolRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private CategoriaInstanciaRepository categoriaInstanciaRepository;
    @Autowired private InstanciaComunRepository instanciaComunRepository;
    @Autowired private ComentarioConfidencialInstanciaRepository comentarioRepository;

    // @Transactional acá dispara el rollback automático de los tests de Spring al
    // finalizar cada método: la fila de prueba nunca queda persistida de verdad.
    @Test
    @Transactional
    void guardarComentarioConfidencialInstancia_PersisteConIdCompuestoCorrecto() {
        Rol rolAdmin = rolRepository.findByNombre("ADMINISTRADOR")
                .orElseThrow(() -> new IllegalStateException(
                        "Rol ADMINISTRADOR no encontrado — ¿se corrió proyecto_schema.sql?"));

        List<CategoriaInstancia> categorias = categoriaInstanciaRepository.findAll();
        if (categorias.isEmpty()) {
            throw new IllegalStateException("No hay categorías de instancia — ¿se corrió proyecto_schema.sql?");
        }
        CategoriaInstancia categoria = categorias.get(0);

        Estudiante estudiante = new Estudiante(
                "Test", "Estudiante IT", "90000001", PaisDocumento.URY,
                "hash", "it.estudiante@utec.edu.uy", null, null,
                null, null, null, null,
                LocalDate.of(2000, 1, 1), null, null, null, null);
        estudiante.setEstado(EstadoLogico.ACTIVO);
        estudiante.setRol(rolRepository.findByNombre("ESTUDIANTE")
                .orElseThrow(() -> new IllegalStateException(
                        "Rol ESTUDIANTE no encontrado — ¿se corrió proyecto_schema.sql?")));
        estudiante = estudianteRepository.save(estudiante);

        Funcionario autor = new Funcionario(
                "Test", "Autor IT", "90000002", PaisDocumento.URY,
                "hash", "it.autor@utec.edu.uy", rolAdmin);
        autor.setEstado(EstadoLogico.ACTIVO);
        autor = funcionarioRepository.save(autor);

        InstanciaComun instancia = new InstanciaComun(
                "Instancia de integración", LocalDateTime.now().plusDays(1),
                estudiante, autor, "Presencial", categoria);
        instancia = instanciaComunRepository.save(instancia);

        ComentarioConfidencialInstancia comentario = new ComentarioConfidencialInstancia();
        comentario.setId(new ComentarioConfidencialInstancia.ComentarioConfidencialInstanciaId());
        comentario.setInstancia(instancia);
        comentario.setContenido("Observación confidencial de integración (M15)");
        comentario.setAutor(autor);

        ComentarioConfidencialInstancia guardado = comentarioRepository.save(comentario);

        // La clave compuesta se generó y quedó mapeada a la instancia correcta
        assertNotNull(guardado.getId());
        assertNotNull(guardado.getId().getIdComConfidencial());
        assertEquals(instancia.getCodInstancia(), guardado.getId().getCodInstancia());
        assertEquals(instancia.getCodInstancia(), guardado.getInstancia().getCodInstancia());

        // Se puede recuperar de nuevo por su clave real (no solo por la referencia en memoria)
        Optional<ComentarioConfidencialInstancia> recargado = comentarioRepository
                .findById_IdComConfidencialAndEstado(guardado.getId().getIdComConfidencial(), EstadoLogico.ACTIVO);
        assertTrue(recargado.isPresent());
        assertEquals("Observación confidencial de integración (M15)", recargado.get().getContenido());

        Page<ComentarioConfidencialInstancia> porInstancia = comentarioRepository
                .findByInstancia_CodInstanciaAndEstadoIn(instancia.getCodInstancia(),
                        List.of(EstadoLogico.ACTIVO, EstadoLogico.ELIMINADO), PageRequest.of(0, 20));
        assertEquals(1, porInstancia.getTotalElements());
    }
}
