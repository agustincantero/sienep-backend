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
                .orElseThrow(() -> new EntityNotFoundException("No se encontró un estudiante con el id " + dto.getIdEstudiante()));

        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró un funcionario con el id " + dto.getIdFuncionario()));

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
        return toResponseDTO(buscarActivaOFallar(id));
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
        Incidencia incidencia = buscarActivaOFallar(id);

        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró un estudiante con el id " + dto.getIdEstudiante()));

        Funcionario funcionario = funcionarioRepository.findById(dto.getIdFuncionario())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró un funcionario con el id " + dto.getIdFuncionario()));

        incidencia.setTitulo(dto.getTitulo());
        incidencia.setFechaHora(dto.getFechaHora());
        incidencia.setEstudiante(estudiante);
        incidencia.setFuncionario(funcionario);
        incidencia.setCanal(dto.getCanal());
        incidencia.setLugar(dto.getLugar());

        return toResponseDTO(incidenciaRepository.save(incidencia));
    }

    public void darDeBaja(int id) {
        Incidencia incidencia = buscarActivaOFallar(id);
        incidencia.setEstado(EstadoLogico.INACTIVO);
        incidenciaRepository.save(incidencia);
    }

    private Incidencia buscarActivaOFallar(int id) {
        return incidenciaRepository.findByCodInstanciaAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una incidencia activa con el id " + id));
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