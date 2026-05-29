package com.fides.sienep.service;

import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Rol;
import com.fides.sienep.model.Usuario;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FuncionarioService {

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    // LISTAR TODOS

    public List<FuncionarioResponseDTO> listar() {

        List<Funcionario> funcionarios =
                funcionarioRepository.findAll();

        List<FuncionarioResponseDTO> respuesta =
                new ArrayList<>();

        for (Funcionario funcionario : funcionarios) {

            FuncionarioResponseDTO dto =
                    new FuncionarioResponseDTO();

            dto.setIdUsuario(
                    funcionario.getIdUsuario()
            );

            dto.setNombreUsuario(
                    funcionario.getUsuario().getNombre()
            );

            dto.setApellidoUsuario(
                    funcionario.getUsuario().getApellido()
            );

            dto.setNombreRol(
                    funcionario.getRol().getNombre()
            );

            respuesta.add(dto);
        }

        return respuesta;
    }


    // OBTENER POR ID

    public FuncionarioResponseDTO obtenerPorId(
            Integer id
    ) {

        Optional<Funcionario> optional =
                funcionarioRepository.findById(id);

        if (optional.isEmpty()) {
            return null;
        }

        Funcionario funcionario = optional.get();

        FuncionarioResponseDTO dto =
                new FuncionarioResponseDTO();

        dto.setIdUsuario(
                funcionario.getIdUsuario()
        );

        dto.setNombreUsuario(
                funcionario.getUsuario().getNombre()
        );

        dto.setApellidoUsuario(
                funcionario.getUsuario().getApellido()
        );

        dto.setNombreRol(
                funcionario.getRol().getNombre()
        );

        return dto;
    }

    // CREAR

    public FuncionarioResponseDTO crear(
            FuncionarioRequestDTO dto
    ) {

        Optional<Usuario> usuarioOptional =
                usuarioRepository.findById(
                        dto.getIdUsuario()
                );

        Optional<Rol> rolOptional =
                rolRepository.findById(
                        dto.getIdRol()
                );

        if (usuarioOptional.isEmpty()
                || rolOptional.isEmpty()) {

            return null;
        }

        Funcionario funcionario =
                new Funcionario();

        funcionario.setUsuario(
                usuarioOptional.get()
        );

        funcionario.setRol(
                rolOptional.get()
        );

        Funcionario guardado =
                funcionarioRepository.save(
                        funcionario
                );

        FuncionarioResponseDTO response =
                new FuncionarioResponseDTO();

        response.setIdUsuario(
                guardado.getIdUsuario()
        );

        response.setNombreUsuario(
                guardado.getUsuario().getNombre()
        );

        response.setApellidoUsuario(
                guardado.getUsuario().getApellido()
        );

        response.setNombreRol(
                guardado.getRol().getNombre()
        );

        return response;
    }

    // ELIMINAR

    public void eliminar(Integer id) {

        funcionarioRepository.deleteById(id);
    }
}