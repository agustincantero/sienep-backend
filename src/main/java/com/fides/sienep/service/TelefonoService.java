package com.fides.sienep.service;

import com.fides.sienep.dto.TelefonoRequestDTO;
import com.fides.sienep.dto.TelefonoResponseDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Telefono;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.TelefonoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelefonoService {

    private static final Logger log = LoggerFactory.getLogger(TelefonoService.class);

    private final TelefonoRepository telefonoRepository;
    private final EstudianteRepository estudianteRepository;

    @Transactional(readOnly = true)
    public List<TelefonoResponseDTO> listarPorEstudiante(Integer idUsuario) {
        List<Telefono> telefonos = telefonoRepository
                .findByIdIdUsuarioAndEstado(idUsuario, EstadoLogico.ACTIVO);
        List<TelefonoResponseDTO> resultado = new ArrayList<>();
        for (Telefono t : telefonos) {
            resultado.add(toDTO(t));
        }
        return resultado;
    }

    @Transactional
    public TelefonoResponseDTO agregar(TelefonoRequestDTO dto) {
        Estudiante estudiante = estudianteRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new RuntimeException(
                        "Estudiante no encontrado: " + dto.getIdUsuario()));

        Telefono.Id id = new Telefono.Id(dto.getIdUsuario(), dto.getNumero());
        if (telefonoRepository.existsById(id)) {
            throw new RuntimeException("El teléfono ya existe para este estudiante");
        }

        Telefono telefono = new Telefono(estudiante, dto.getNumero());
        telefonoRepository.save(telefono);
        log.info("Teléfono agregado al estudiante {}", dto.getIdUsuario());
        return toDTO(telefono);
    }

    @Transactional
    public void eliminar(Integer idUsuario, String numero) {
        Telefono.Id id = new Telefono.Id(idUsuario, numero);
        Telefono telefono = telefonoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Teléfono no encontrado"));
        telefono.setEstado(EstadoLogico.ELIMINADO);
        log.info("Teléfono {} del estudiante {} dado de baja", numero, idUsuario);
    }

    private TelefonoResponseDTO toDTO(Telefono t) {
        TelefonoResponseDTO dto = new TelefonoResponseDTO();
        dto.setIdUsuario(t.getId().getIdUsuario());
        dto.setNumero(t.getId().getNumero());
        dto.setEstado(t.getEstado());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
    }
}