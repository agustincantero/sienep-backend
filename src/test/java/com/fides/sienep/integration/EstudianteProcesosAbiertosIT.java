package com.fides.sienep.integration;

import com.fides.sienep.dto.ProcesosAbiertosDTO;
import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.model.TipoRecordatorio;
import com.fides.sienep.repository.CategoriaInstanciaRepository;
import com.fides.sienep.repository.CategoriaRecordatorioRepository;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.repository.RecordatorioRepository;
import com.fides.sienep.repository.RolRepository;
import com.fides.sienep.service.EstudianteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test de integración real contra Postgres (requiere `docker compose up -d` con el schema ya aplicado). Igual que
// ComentarioConfidencialInstanciaIT, el sufijo "IT" lo deja fuera de `./mvnw test`; se corre con:
//
//   docker compose up -d
//   ./mvnw test -Dtest=EstudianteProcesosAbiertosIT -Dspring.profiles.active=integration
//
// Motivo: RF06 — que el conteo de procesos abiertos antes de la baja cuente solo lo que corresponde (instancias
// futuras y activas, recordatorios activos no cumplidos), y no lo pasado, lo cumplido, lo inactivo ni lo de otro
// estudiante. @Transactional: todo lo creado se deshace al terminar.
@SpringBootTest
@ActiveProfiles("integration")
@Transactional
class EstudianteProcesosAbiertosIT {

    @Autowired private EstudianteService estudianteService;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private FuncionarioRepository funcionarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private CategoriaInstanciaRepository categoriaInstanciaRepository;
    @Autowired private CategoriaRecordatorioRepository categoriaRecordatorioRepository;
    @Autowired private InstanciaComunRepository instanciaComunRepository;
    @Autowired private RecordatorioRepository recordatorioRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Estudiante estudiante(String documento) {
        Estudiante e = new Estudiante("Procesos", "IT " + documento, documento, PaisDocumento.ARG, "hash",
                documento + "@it.utec.edu.uy", null, null, null, null, null, null,
                LocalDate.of(2000, 1, 1), null, null, null, null);
        e.setEstado(EstadoLogico.ACTIVO);
        e.setRol(rolRepository.findByNombre("ESTUDIANTE").orElseThrow());
        return estudianteRepository.save(e);
    }

    private InstanciaComun instancia(Estudiante estudiante, Funcionario responsable, LocalDateTime fecha,
                                     EstadoLogico estado) {
        CategoriaInstancia categoria = categoriaInstanciaRepository.findAll().get(0);
        InstanciaComun i = new InstanciaComun("Instancia IT", fecha, estudiante, responsable, "Presencial", categoria);
        i.setEstado(estado);
        return instanciaComunRepository.save(i);
    }

    private void recordatorio(InstanciaComun instancia, Funcionario responsable, EstadoLogico estado, boolean cumplido) {
        Recordatorio r = new Recordatorio("Recordatorio IT", TipoRecordatorio.REUNION,
                LocalDateTime.now().plusDays(3), instancia,
                categoriaRecordatorioRepository.findAll().get(0), responsable);
        r.setEstado(estado);
        r.setCumplido(cumplido);
        recordatorioRepository.save(r);
    }

    @Test
    @DisplayName("Cuenta solo instancias futuras activas y recordatorios activos no cumplidos del estudiante")
    void procesosAbiertos_CuentaSoloLoQueCorresponde() {
        Funcionario responsable = new Funcionario("Resp", "IT", "90000201", PaisDocumento.ARG, "hash",
                "90000201@it.utec.edu.uy", rolRepository.findByNombre("ADMINISTRADOR").orElseThrow());
        responsable.setEstado(EstadoLogico.ACTIVO);
        responsable = funcionarioRepository.save(responsable);

        Estudiante estudiante = estudiante("90000202");
        Estudiante otro = estudiante("90000203");

        // Cuentan: 2 instancias futuras activas.
        InstanciaComun futura = instancia(estudiante, responsable, LocalDateTime.now().plusDays(5), EstadoLogico.ACTIVO);
        instancia(estudiante, responsable, LocalDateTime.now().plusDays(10), EstadoLogico.ACTIVO);
        // No cuentan: pasada, futura pero inactiva, y una futura de otro estudiante.
        InstanciaComun pasada = instancia(estudiante, responsable, LocalDateTime.now().minusDays(5), EstadoLogico.ACTIVO);
        instancia(estudiante, responsable, LocalDateTime.now().plusDays(5), EstadoLogico.INACTIVO);
        InstanciaComun deOtro = instancia(otro, responsable, LocalDateTime.now().plusDays(5), EstadoLogico.ACTIVO);

        // Cuentan: 2 recordatorios activos no cumplidos (uno colgado de una instancia ya pasada: sigue pendiente).
        recordatorio(futura, responsable, EstadoLogico.ACTIVO, false);
        recordatorio(pasada, responsable, EstadoLogico.ACTIVO, false);
        // No cuentan: cumplido, inactivo y uno de otro estudiante.
        recordatorio(futura, responsable, EstadoLogico.ACTIVO, true);
        recordatorio(futura, responsable, EstadoLogico.INACTIVO, false);
        recordatorio(deOtro, responsable, EstadoLogico.ACTIVO, false);

        // Escribe los datos sin usuario autenticado (ver EstudianteBusquedaIT: si no, el trigger de auditoría tomaría
        // como autor al usuario simulado, que no existe en la base).
        recordatorioRepository.flush();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "1", null, List.of(new SimpleGrantedAuthority("DESACTIVAR_ESTUDIANTE"))));

        ProcesosAbiertosDTO procesos = estudianteService.procesosAbiertos(estudiante.getIdUsuario());
        assertEquals(2, procesos.getInstanciasFuturas());
        assertEquals(2, procesos.getRecordatoriosPendientes());

        // La baja sin confirmar se frena (409) y el estudiante sigue activo.
        int id = estudiante.getIdUsuario();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> estudianteService.desactivar(id, false));
        assertEquals(409, ex.getStatusCode().value());
        assertEquals(EstadoLogico.ACTIVO, estudianteRepository.findById(id).orElseThrow().getEstado());

        // Al otro estudiante solo se le cuenta lo suyo: su instancia futura y el recordatorio de esa instancia.
        ProcesosAbiertosDTO delOtro = estudianteService.procesosAbiertos(otro.getIdUsuario());
        assertEquals(1, delOtro.getInstanciasFuturas());
        assertEquals(1, delOtro.getRecordatoriosPendientes());
    }
}
