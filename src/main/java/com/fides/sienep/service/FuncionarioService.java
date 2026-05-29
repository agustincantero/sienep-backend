package com.fides.sienep.service;

import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Rol;
import com.fides.sienep.model.Usuario;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.RolRepository;
import com.fides.sienep.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;

    private final UsuarioRepository usuarioRepository;

    private final RolRepository rolRepository;

    @Transactional
    public FuncionarioResponseDTO crear(FuncionarioRequestDTO dto) {

        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));

        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Rol no encontrado"
                ));

        Funcionario funcionario = new Funcionario();

        funcionario.setUsuario(usuario);

        funcionario.setRol(rol);

        funcionario = funcionarioRepository.save(funcionario);

        return toResponseDTO(funcionario);
    }

    @Transactional(readOnly = true)
    public FuncionarioResponseDTO obtenerPorId(Integer id) {

        return toResponseDTO(buscarOFallar(id));
    }

    private Funcionario buscarOFallar(Integer id) {

        return funcionarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Funcionario no encontrado"
                        ));
    }

    private FuncionarioResponseDTO toResponseDTO(Funcionario f) {

        return FuncionarioResponseDTO.builder()
                .idUsuario(f.getIdUsuario())
                .nombre(f.getUsuario().getNombre())
                .apellido(f.getUsuario().getApellido())
                .idRol(f.getRol().getIdRol())
                .nombreRol(f.getRol().getNomRol())
                .build();
    }
}