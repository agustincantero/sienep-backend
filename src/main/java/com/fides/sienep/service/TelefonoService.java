package com.fides.sienep.service;

import com.fides.sienep.dto.TelefonoRequestDTO;
import com.fides.sienep.dto.TelefonoResponseDTO;
import com.fides.sienep.mapper.TelefonoMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Telefono;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.TelefonoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TelefonoService {

    private static final Logger log = LoggerFactory.getLogger(TelefonoService.class);

    private final TelefonoRepository telefonoRepository;
    private final EstudianteRepository estudianteRepository;
    private final TelefonoMapper telefonoMapper;

    @PreAuthorize("hasAuthority('VER_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public List<TelefonoResponseDTO> listarPorEstudiante(Integer idUsuario) {
        List<Telefono> telefonos = telefonoRepository.findByIdIdUsuarioAndEstado(idUsuario, EstadoLogico.ACTIVO);
        return telefonoMapper.toDtoList(telefonos);
    }

    @PreAuthorize("hasAuthority('EDITAR_ESTUDIANTE')")
    @Transactional
    public TelefonoResponseDTO agregar(TelefonoRequestDTO dto) {
        Estudiante estudiante = estudianteRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Estudiante no encontrado: " + dto.getIdUsuario()));

        Telefono.Id id = new Telefono.Id(dto.getIdUsuario(), dto.getNumero());
        if (telefonoRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El teléfono ya existe para este estudiante");
        }

        Telefono telefono = new Telefono(estudiante, dto.getNumero());
        telefonoRepository.save(telefono);
        log.info("[AUDIT] Teléfono agregado al estudiante {}", dto.getIdUsuario());
        return telefonoMapper.toDto(telefono);
    }

    @PreAuthorize("hasAuthority('ELIMINAR_ESTUDIANTE')")
    @Transactional
    public void eliminar(Integer idUsuario, String numero) {
        Telefono.Id id = new Telefono.Id(idUsuario, numero);
        Telefono telefono = telefonoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Teléfono no encontrado"));
        telefono.setEstado(EstadoLogico.ELIMINADO);
        log.info("[AUDIT] Teléfono {} del estudiante {} dado de baja", numero, idUsuario);
    }
}