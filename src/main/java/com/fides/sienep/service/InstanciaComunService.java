package com.fides.sienep.service;

import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.dto.InstanciaComunUpdateDTO;
import com.fides.sienep.mapper.InstanciaComunMapper;
import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.repository.CategoriaInstanciaRepository;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.utils.InstanciaIdGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstanciaComunService {

    private static final Logger log = LoggerFactory.getLogger(InstanciaComunService.class);

    private final InstanciaComunRepository instanciaComunRepository;
    private final EstudianteRepository estudianteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final CategoriaInstanciaRepository categoriaInstanciaRepository;
    private final InstanciaIdGenerator instanciaIdGenerator;
    private final InstanciaComunMapper instanciaComunMapper;
    private final EmailService emailService;

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(auth.getName());
    }

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_INSTANCIA')")
    public InstanciaComunResponseDTO crear(InstanciaComunRequestDTO dto) {
        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un estudiante con el id " + dto.getIdEstudiante()));

        if (estudiante.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "El estudiante con id " + dto.getIdEstudiante() + " no está activo");
        }

        Funcionario funcionario = funcionarioRepository.findById(getIdUsuarioAutenticado())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el funcionario autenticado"));

        if (funcionario.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "El funcionario autenticado no está activo");
        }

        CategoriaInstancia categoria = categoriaInstanciaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        if (categoria.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "La categoría con id " + dto.getIdCategoria() + " no está activa");
        }

        InstanciaComun instancia = new InstanciaComun(
                dto.getTitulo(),
                dto.getFechaHora(),
                estudiante,
                funcionario,
                dto.getCanal(),
                categoria
        );

        instancia.setIdNegInstancia(instanciaIdGenerator.generarIdNeg());

        InstanciaComunResponseDTO response = instanciaComunMapper.toDto(
                instanciaComunRepository.save(instancia));

        // RF15 — notificar al funcionario creador con el ID de negocio generado.
        // El try-catch externo es una segunda capa de defensa: aunque enviarNotificacionInstancia
        // ya no relanza, esto garantiza que cualquier excepción inesperada no revierta
        // la transacción de @Transactional.
        try {
            emailService.enviarNotificacionInstancia(
                    funcionario.getEmail(),
                    funcionario.getNombre(),
                    response.getIdNegInstancia(),
                    response.getTitulo()
            );
        } catch (Exception e) {
            log.warn("[AUDIT] Fallo al notificar instancia {} al funcionario {}: {}",
                    response.getIdNegInstancia(), funcionario.getIdUsuario(), e.getMessage());
        }

        return response;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public List<InstanciaComunResponseDTO> listarActivas() {
        return instanciaComunMapper.toDtoList(
                instanciaComunRepository.findByEstado(EstadoLogico.ACTIVO));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public InstanciaComunResponseDTO buscarPorId(int codInstancia) {
        return instanciaComunMapper.toDto(buscarActivaOFallar(codInstancia));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public List<InstanciaComunResponseDTO> listarPorEstudiante(int idEstudiante) {
        return instanciaComunMapper.toDtoList(
                instanciaComunRepository.findByEstudianteAndEstado(idEstudiante, EstadoLogico.ACTIVO));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public List<InstanciaComunResponseDTO> listarPorFuncionario(int idFuncionario) {
        return instanciaComunMapper.toDtoList(
                instanciaComunRepository.findByFuncionarioAndEstado(idFuncionario, EstadoLogico.ACTIVO));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public List<InstanciaComunResponseDTO> listarPorCategoria(int idCategoria) {
        return instanciaComunMapper.toDtoList(
                instanciaComunRepository.findByCategoriaAndEstado(idCategoria, EstadoLogico.ACTIVO));
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public List<InstanciaComunResponseDTO> listarMisInstancias() {
        Integer idEstudiante = getIdUsuarioAutenticado();
        return instanciaComunMapper.toDtoList(
                instanciaComunRepository.findByEstudianteAndEstado(idEstudiante, EstadoLogico.ACTIVO));
    }

    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_INSTANCIA')")
    public InstanciaComunResponseDTO actualizar(int codInstancia, InstanciaComunUpdateDTO dto) {
        InstanciaComun instancia = buscarActivaOFallar(codInstancia);

        CategoriaInstancia categoria = categoriaInstanciaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        if (categoria.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "La categoría con id " + dto.getIdCategoria() + " no está activa");
        }

        instancia.setTitulo(dto.getTitulo());
        instancia.setFechaHora(dto.getFechaHora());
        instancia.setCanal(dto.getCanal());
        instancia.setCategoria(categoria);

        return instanciaComunMapper.toDto(instanciaComunRepository.save(instancia));
    }

    @Transactional
    @PreAuthorize("hasAuthority('BAJA_INSTANCIA')")
    public void darDeBaja(int codInstancia) {
        InstanciaComun instancia = buscarActivaOFallar(codInstancia);
        instancia.setEstado(EstadoLogico.INACTIVO);
        instanciaComunRepository.save(instancia);
    }

    // ----------------------------------------------------------------
    // Helpers privados
    // ----------------------------------------------------------------

    private InstanciaComun buscarActivaOFallar(int codInstancia) {
        return instanciaComunRepository.findByCodInstanciaAndEstado(codInstancia, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una instancia activa con el id " + codInstancia));
    }
}