package com.fides.sienep.service;

import com.fides.sienep.dto.IncidenciaRequestDTO;
import com.fides.sienep.dto.IncidenciaResponseDTO;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Incidencia;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.IncidenciaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaRepository incidenciaRepository;
    private final EstudianteRepository estudianteRepository;
    private final FuncionarioRepository funcionarioRepository;

    public IncidenciaResponseDTO crear(IncidenciaRequestDTO dto) {
        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new EntityNotFoundException("Estudiante con id " + dto.getIdEstudiante() + " no encontrado"));

        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new EntityNotFoundException("Funcionario con id " + dto.getIdFuncionario() + " no encontrado"));

        Incidencia incidencia = new Incidencia(
                dto.getTitulo(),
                dto.getFechaHora(),
                estudiante,
                funcionario,
                dto.getCanal(),
                dto.getLugar()
        );

        return toResponseDTO(incidenciaRepository.save(incidencia));
    }

    public List<IncidenciaResponseDTO> listarActivas() {
        List<Incidencia> incidencias = incidenciaRepository.findByEstado(EstadoLogico.ACTIVO);
        List<IncidenciaResponseDTO> resultado = new ArrayList<>();
        for (Incidencia incidencia : incidencias) {
            resultado.add(toResponseDTO(incidencia));
        }
        return resultado;
    }

    public IncidenciaResponseDTO buscarPorId(int id) {
        Incidencia incidencia = incidenciaRepository.findByCodInstanciaAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new EntityNotFoundException("Incidencia con id " + id + " no encontrada"));
        return toResponseDTO(incidencia);
    }

    public List<IncidenciaResponseDTO> listarPorEstudiante(int idEstudiante) {
        List<Incidencia> incidencias = incidenciaRepository.findByEstudianteAndEstado(idEstudiante, EstadoLogico.ACTIVO);
        List<IncidenciaResponseDTO> resultado = new ArrayList<>();
        for (Incidencia incidencia : incidencias) {
            resultado.add(toResponseDTO(incidencia));
        }
        return resultado;
    }

    public List<IncidenciaResponseDTO> listarPorFuncionario(int idFuncionario) {
        List<Incidencia> incidencias = incidenciaRepository.findByFuncionarioAndEstado(idFuncionario, EstadoLogico.ACTIVO);
        List<IncidenciaResponseDTO> resultado = new ArrayList<>();
        for (Incidencia incidencia : incidencias) {
            resultado.add(toResponseDTO(incidencia));
        }
        return resultado;
    }

    public IncidenciaResponseDTO actualizar(int id, IncidenciaRequestDTO dto) {
        Incidencia incidencia = incidenciaRepository.findByCodInstanciaAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new EntityNotFoundException("Incidencia con id " + id + " no encontrada"));

        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new EntityNotFoundException("Estudiante con id " + dto.getIdEstudiante() + " no encontrado"));

        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new EntityNotFoundException("Funcionario con id " + dto.getIdFuncionario() + " no encontrado"));

        incidencia.setTitulo(dto.getTitulo());
        incidencia.setFechaHora(dto.getFechaHora());
        incidencia.setEstudiante(estudiante);
        incidencia.setFuncionario(funcionario);
        incidencia.setCanal(dto.getCanal());
        incidencia.setLugar(dto.getLugar());

        return toResponseDTO(incidenciaRepository.save(incidencia));
    }

    public void darDeBaja(int id) {
        Incidencia incidencia = incidenciaRepository.findByCodInstanciaAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new EntityNotFoundException("Incidencia con id " + id + " no encontrada"));
        incidencia.setEstado(EstadoLogico.INACTIVO);
        incidenciaRepository.save(incidencia);
    }

    private IncidenciaResponseDTO toResponseDTO(Incidencia incidencia) {
        IncidenciaResponseDTO dto = new IncidenciaResponseDTO();
        dto.setCodInstancia(incidencia.getCodInstancia());
        dto.setTitulo(incidencia.getTitulo());
        dto.setFechaHora(incidencia.getFechaHora());
        dto.setIdEstudiante(incidencia.getEstudiante().getIdUsuario());
        dto.setNombreEstudiante(incidencia.getEstudiante().getNombre() + " " + incidencia.getEstudiante().getApellido());
        dto.setIdFuncionario(incidencia.getFuncionario().getIdUsuario());
        dto.setNombreFuncionario(incidencia.getFuncionario().getNombre() + " " + incidencia.getFuncionario().getApellido());
        dto.setCanal(incidencia.getCanal());
        dto.setIdNegInstancia(incidencia.getIdNegInstancia());
        dto.setLugar(incidencia.getLugar());
        dto.setEstado(incidencia.getEstado());
        dto.setCreatedAt(incidencia.getCreatedAt());
        dto.setUpdatedAt(incidencia.getUpdatedAt());
        return dto;
    }
}