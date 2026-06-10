package com.fides.sienep.service;

import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// ─────────────────────────────────────────────────────────────
// CAMBIO 1 — @Autowired → @RequiredArgsConstructor
//
// @Autowired en campo es inyección por campo (field injection).
// Funciona, pero tiene un problema grave: el compilador no puede
// garantizar que el objeto esté completamente construido antes de
// usarlo. Si arrancás en un contexto de test sin Spring, los campos
// quedan null.
//
// La buena práctica actual es constructor injection:
//   - Los campos son final → el compilador los exige en la
//     construcción, imposible olvidar uno.
//   - Hace la clase testeable sin Spring (MockitoExtension crea el
//     objeto con new, pasándole los mocks).
//   - Lombok genera el constructor automáticamente con
//     @RequiredArgsConstructor (todos los campos final).
// ─────────────────────────────────────────────────────────────
@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final RolRepository rolRepository;

    // OBTENER TODOS
    public List<FuncionarioResponseDTO> obtenerTodos() {

        List<Funcionario> funcionarios = funcionarioRepository.findAll();

        List<FuncionarioResponseDTO> respuesta = new ArrayList<>();

        for (Funcionario funcionario : funcionarios) {

            FuncionarioResponseDTO dto = convertirAResponseDTO(funcionario);

            respuesta.add(dto);
        }

        return respuesta;
    }

    // OBTENER POR ID
    public FuncionarioResponseDTO obtenerPorId(Integer id) {

        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Funcionario no encontrado: " + id));

        return convertirAResponseDTO(funcionario);   // ← esta línea tiene que estar
    }

    // CREAR
    public FuncionarioResponseDTO crear(FuncionarioRequestDTO dto) {

        Funcionario funcionario = new Funcionario();

        funcionario.setNombre(dto.getNombre());
        funcionario.setApellido(dto.getApellido());
        funcionario.setEmail(dto.getEmail());
        funcionario.setDocumento(dto.getDocumento());

        Optional<Rol> rolOptional =
                rolRepository.findById(dto.getIdRol());

        if (rolOptional.isPresent()) {

            funcionario.setRol(rolOptional.get());
        }

        Funcionario guardado =
                funcionarioRepository.save(funcionario);

        return convertirAResponseDTO(guardado);
    }

    // ACTUALIZAR
    public FuncionarioResponseDTO actualizar(Integer id, FuncionarioRequestDTO dto) {

        // CAMBIO 2 — mismo tratamiento que obtenerPorId
        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Funcionario no encontrado: " + id
                ));

        funcionario.setNombre(dto.getNombre());
        funcionario.setApellido(dto.getApellido());
        funcionario.setEmail(dto.getEmail());
        funcionario.setDocumento(dto.getDocumento());

        Optional<Rol> rolOptional =
                rolRepository.findById(dto.getIdRol());

        if (rolOptional.isPresent()) {

            funcionario.setRol(rolOptional.get());
        }

        Funcionario actualizado =
                funcionarioRepository.save(funcionario);

        return convertirAResponseDTO(actualizado);
    }

    // ELIMINAR
    public void eliminar(Integer id) {

        funcionarioRepository.deleteById(id);
    }

    // METODO AUXILIAR
    private FuncionarioResponseDTO convertirAResponseDTO(Funcionario funcionario) {

        FuncionarioResponseDTO dto = new FuncionarioResponseDTO();

        dto.setIdUsuario(funcionario.getIdUsuario());
        dto.setNombre(funcionario.getNombre());
        dto.setApellido(funcionario.getApellido());
        dto.setEmail(funcionario.getEmail());

        if (funcionario.getRol() != null) {

            dto.setRol(funcionario.getRol().getNombre());
        }

        return dto;
    }
}