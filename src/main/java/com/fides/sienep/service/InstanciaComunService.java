package com.fides.sienep.service;

import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.dto.InstanciaComunUpdateDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstanciaComunService {

    private final InstanciaComunRepository instanciaComunRepository;
    private final EstudianteRepository estudianteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final CategoriaInstanciaRepository categoriaInstanciaRepository;
    private final InstanciaIdGenerator instanciaIdGenerator;

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

        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un funcionario con el id " + dto.getIdFuncionario()));

        CategoriaInstancia categoria = categoriaInstanciaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        InstanciaComun instancia = new InstanciaComun(
                dto.getTitulo(),
                dto.getFechaHora(),
                estudiante,
                funcionario,
                dto.getCanal(),
                categoria
        );

        instancia.setIdNegInstancia(instanciaIdGenerator.generarIdNeg());

        return toResponseDTO(instanciaComunRepository.save(instancia));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public List<InstanciaComunResponseDTO> listarActivas() {
        List<InstanciaComun> instancias = instanciaComunRepository.findByEstado(EstadoLogico.ACTIVO);
        List<InstanciaComunResponseDTO> resultado = new ArrayList<>();
        for (InstanciaComun instancia : instancias) {
            resultado.add(toResponseDTO(instancia));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public InstanciaComunResponseDTO buscarPorId(int codInstancia) {
        return toResponseDTO(buscarActivaOFallar(codInstancia));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public List<InstanciaComunResponseDTO> listarPorEstudiante(int idEstudiante) {
        List<InstanciaComun> instancias = instanciaComunRepository
                .findByEstudianteAndEstado(idEstudiante, EstadoLogico.ACTIVO);
        List<InstanciaComunResponseDTO> resultado = new ArrayList<>();
        for (InstanciaComun instancia : instancias) {
            resultado.add(toResponseDTO(instancia));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public List<InstanciaComunResponseDTO> listarPorFuncionario(int idFuncionario) {
        List<InstanciaComun> instancias = instanciaComunRepository
                .findByFuncionarioAndEstado(idFuncionario, EstadoLogico.ACTIVO);
        List<InstanciaComunResponseDTO> resultado = new ArrayList<>();
        for (InstanciaComun instancia : instancias) {
            resultado.add(toResponseDTO(instancia));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public List<InstanciaComunResponseDTO> listarPorCategoria(int idCategoria) {
        List<InstanciaComun> instancias = instanciaComunRepository
                .findByCategoriaAndEstado(idCategoria, EstadoLogico.ACTIVO);
        List<InstanciaComunResponseDTO> resultado = new ArrayList<>();
        for (InstanciaComun instancia : instancias) {
            resultado.add(toResponseDTO(instancia));
        }
        return resultado;
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public List<InstanciaComunResponseDTO> listarMisInstancias() {
        Integer idEstudiante = getIdUsuarioAutenticado();
        List<InstanciaComun> instancias = instanciaComunRepository
                .findByEstudianteAndEstado(idEstudiante, EstadoLogico.ACTIVO);
        List<InstanciaComunResponseDTO> resultado = new ArrayList<>();
        for (InstanciaComun instancia : instancias) {
            resultado.add(toResponseDTO(instancia));
        }
        return resultado;
    }

    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_INSTANCIA')")
    public InstanciaComunResponseDTO actualizar(int codInstancia, InstanciaComunUpdateDTO dto) {
        InstanciaComun instancia = buscarActivaOFallar(codInstancia);

        CategoriaInstancia categoria = categoriaInstanciaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        instancia.setTitulo(dto.getTitulo());
        instancia.setFechaHora(dto.getFechaHora());
        instancia.setCanal(dto.getCanal());
        instancia.setCategoria(categoria);

        return toResponseDTO(instanciaComunRepository.save(instancia));
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

    private InstanciaComunResponseDTO toResponseDTO(InstanciaComun ic) {
        InstanciaComunResponseDTO dto = new InstanciaComunResponseDTO();
        dto.setCodInstancia(ic.getCodInstancia());
        dto.setIdNegInstancia(ic.getIdNegInstancia());
        dto.setTitulo(ic.getTitulo());
        dto.setFechaHora(ic.getFechaHora());
        dto.setCanal(ic.getCanal());
        dto.setIdEstudiante(ic.getEstudiante().getIdUsuario());
        dto.setNombreEstudiante(ic.getEstudiante().getNombre() + " " + ic.getEstudiante().getApellido());
        dto.setIdFuncionario(ic.getFuncionario().getIdUsuario());
        dto.setNombreFuncionario(ic.getFuncionario().getNombre() + " " + ic.getFuncionario().getApellido());
        dto.setIdCategoria(ic.getCategoria().getIdCategoria());
        dto.setNombreCategoria(ic.getCategoria().getNomCategoria());
        dto.setEstado(ic.getEstado());
        dto.setCreatedAt(ic.getCreatedAt());
        dto.setUpdatedAt(ic.getUpdatedAt());
        return dto;
    }
}