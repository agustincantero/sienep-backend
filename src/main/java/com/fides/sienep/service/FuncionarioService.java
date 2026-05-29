package com.fides.sienep.service;

import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Rol;
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
    private RolRepository rolRepository;

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

        Optional<Funcionario> funcionarioOptional =
                funcionarioRepository.findById(id);

        if (funcionarioOptional.isEmpty()) {
            return null;
        }

        Funcionario funcionario = funcionarioOptional.get();

        return convertirAResponseDTO(funcionario);
    }

    // CREAR
    public FuncionarioResponseDTO crear(
            FuncionarioRequestDTO dto
    ) {

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
    public FuncionarioResponseDTO actualizar(
            Integer id,
            FuncionarioRequestDTO dto
    ) {

        Optional<Funcionario> funcionarioOptional =
                funcionarioRepository.findById(id);

        if (funcionarioOptional.isEmpty()) {
            return null;
        }

        Funcionario funcionario = funcionarioOptional.get();

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
    private FuncionarioResponseDTO convertirAResponseDTO(
            Funcionario funcionario
    ) {

        FuncionarioResponseDTO dto =
                new FuncionarioResponseDTO();

        dto.setIdUsuario(funcionario.getIdUsuario());

        dto.setNombre(funcionario.getNombre());
        dto.setApellido(funcionario.getApellido());
        dto.setEmail(funcionario.getEmail());

        if (funcionario.getRol() != null) {

            dto.setRol(
                    funcionario.getRol().getNombre()
            );
        }

        return dto;
    }
}