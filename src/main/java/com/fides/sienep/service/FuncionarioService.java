package com.fides.sienep.service;

import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.dto.FuncionarioUpdateDTO;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.RolRepository;
import com.fides.sienep.utils.ValidadorDeCedulasUy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    // =========================================================
    // CREAR
    // =========================================================

    @PreAuthorize("hasAuthority('CREAR_FUNCIONARIO')")
    @Transactional
    public FuncionarioResponseDTO crear(FuncionarioRequestDTO dto) {

        if (dto.getPaisDocumento() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "paisDocumento es obligatorio"
            );
        }

        if (funcionarioRepository.existsByDocumento(dto.getDocumento())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un funcionario con ese documento."
            );
        }

        if (funcionarioRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un usuario con ese email."
            );
        }

        validarDocumento(dto);

        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Rol no encontrado: " + dto.getIdRol()
                ));

        Funcionario funcionario = new Funcionario(
                dto.getNombre(),
                dto.getApellido(),
                dto.getDocumento(),
                dto.getPaisDocumento(),
                passwordEncoder.encode(dto.getContrasenia()),
                dto.getEmail(),
                rol
        );

        return toResponseDTO(
                funcionarioRepository.save(funcionario)
        );
    }

    // =========================================================
    // CONSULTAS
    // =========================================================

    @PreAuthorize("hasAuthority('VER_FUNCIONARIOS')")
    @Transactional(readOnly = true)
    public FuncionarioResponseDTO obtenerPorId(Integer id) {
        return toResponseDTO(
                buscarOFallar(id)
        );
    }

    @PreAuthorize("hasAuthority('VER_FUNCIONARIOS')")
    @Transactional(readOnly = true)
    public FuncionarioResponseDTO obtenerPorDocumento(String documento) {
        Funcionario funcionario = funcionarioRepository
                .findByDocumentoAndEstado(documento, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new RuntimeException("Funcionario no encontrado"));
        return toResponseDTO(funcionario);
    }

    @PreAuthorize("hasAuthority('VER_FUNCIONARIOS')")
    @Transactional(readOnly = true)
    public List<FuncionarioResponseDTO> obtenerTodos() {

        List<FuncionarioResponseDTO> respuesta =
                new ArrayList<>();

        List<Funcionario> funcionarios =
                funcionarioRepository.findAll();

        for (Funcionario funcionario : funcionarios) {
            respuesta.add(
                    toResponseDTO(funcionario)
            );
        }

        return respuesta;
    }



    // =========================================================
    // ACTUALIZAR
    // =========================================================

    @PreAuthorize("hasAuthority('EDITAR_FUNCIONARIO')")
    @Transactional
    public FuncionarioResponseDTO actualizar(
            Integer id,
            FuncionarioUpdateDTO dto
    ) {

        Funcionario funcionario =
                buscarOFallar(id);

        if (!funcionario.getEmail().equalsIgnoreCase(dto.getEmail())
                && funcionarioRepository.existsByEmail(dto.getEmail())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un usuario con ese email."
            );
        }

        aplicarCambios(funcionario, dto);

        return toResponseDTO(
                funcionarioRepository.save(funcionario)
        );
    }

    // =========================================================
    // BAJA Y REACTIVACIÓN
    // =========================================================

    @PreAuthorize("hasAuthority('ELIMINAR_FUNCIONARIO')")
    @Transactional
    public void darDeBaja(Integer id) {

        Funcionario funcionario =
                buscarOFallar(id);

        funcionario.setEstado(
                EstadoLogico.INACTIVO
        );

        funcionarioRepository.save(funcionario);
    }

    @PreAuthorize("hasAuthority('REACTIVAR_FUNCIONARIO')")
    @Transactional
    public FuncionarioResponseDTO reactivar(Integer id) {

        Funcionario funcionario =
                buscarOFallar(id);

        funcionario.setEstado(
                EstadoLogico.ACTIVO
        );

        funcionarioRepository.save(funcionario);

        return toResponseDTO(funcionario);
    }

    // =========================================================
    // MÉTODOS PRIVADOS
    // =========================================================

    private Funcionario buscarOFallar(Integer id) {

        return funcionarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Funcionario no encontrado: " + id
                        )
                );
    }

    private void aplicarCambios(
            Funcionario funcionario,
            FuncionarioUpdateDTO dto
    ) {

        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Rol no encontrado: " + dto.getIdRol()
                        )
                );

        funcionario.setNombre(dto.getNombre());
        funcionario.setApellido(dto.getApellido());
        funcionario.setEmail(dto.getEmail());
        funcionario.setRol(rol);

        if (dto.getContrasenia() != null && !dto.getContrasenia().isBlank()) {
            funcionario.setContrasenia(passwordEncoder.encode(dto.getContrasenia()));
        }

    }

    private void validarDocumento(
            FuncionarioRequestDTO dto
    ) {

        if (dto.getPaisDocumento() == PaisDocumento.URY
                && !ValidadorDeCedulasUy.esCedulaValida(
                dto.getDocumento()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cédula uruguaya es inválida."
            );
        }
    }

    private FuncionarioResponseDTO toResponseDTO(
            Funcionario funcionario
    ) {

        return FuncionarioResponseDTO.builder()
                .idUsuario(funcionario.getIdUsuario())
                .nombre(funcionario.getNombre())
                .apellido(funcionario.getApellido())
                .documento(funcionario.getDocumento())
                .paisDocumento(funcionario.getPaisDocumento())
                .email(funcionario.getEmail())
                .estado(funcionario.getEstado())
                .rol(
                        funcionario.getRol() != null
                                ? funcionario.getRol().getNombre()
                                : null
                )
                .createdAt(funcionario.getCreatedAt())
                .updatedAt(funcionario.getUpdatedAt())
                .build();
    }
}