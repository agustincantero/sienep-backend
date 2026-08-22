package com.fides.sienep.service;

import com.fides.sienep.dto.CambiarContraseniaDTO;
import com.fides.sienep.dto.FuncionarioRequestDTO;
import com.fides.sienep.dto.FuncionarioResponseDTO;
import com.fides.sienep.dto.FuncionarioUpdateDTO;
import com.fides.sienep.mapper.FuncionarioMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Rol;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.RolRepository;
import com.fides.sienep.utils.ValidadorDeCedulasUy;
import com.fides.sienep.utils.ValidadorDePermisosDeRol;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final FuncionarioMapper funcionarioMapper;
    private final EmailService emailService;
    private final PasswordGeneratorService passwordGeneratorService;
    private static final Logger log = LoggerFactory.getLogger(FuncionarioService.class);

    @PreAuthorize("hasAuthority('CREAR_FUNCIONARIO')")
    @Transactional
    public FuncionarioResponseDTO crear(FuncionarioRequestDTO dto) {

        if (dto.getPaisDocumento() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paisDocumento es obligatorio");
        }

        if (funcionarioRepository.existsByDocumento(dto.getDocumento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un funcionario con ese documento.");
        }

        if (funcionarioRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese email.");
        }

        validarDocumento(dto);

        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado: " + dto.getIdRol()));
        ValidadorDePermisosDeRol.validarActorPuedeAsignarRol(rol);

        String contraseniaPlana = passwordGeneratorService.generar();

        Funcionario funcionario = new Funcionario(
                dto.getNombre(),
                dto.getApellido(),
                dto.getDocumento(),
                dto.getPaisDocumento(),
                passwordEncoder.encode(contraseniaPlana),
                dto.getEmail(),
                rol
        );
        // estado queda PENDIENTE_DE_ACTIVACION por default (definido en el constructor/schema)

        Funcionario guardado = funcionarioRepository.save(funcionario);

        // RF32/RF05 — un fallo al enviar el email no debe impedir el alta: los datos ya son
        // válidos y persistidos, y existe un camino de recuperación (restablecerContrasenia).
        // A diferencia de restablecerContrasenia(), acá no bloqueamos porque la cuenta recién
        // creada no tiene ninguna contraseña previa que se pueda seguir usando mientras
        // tanto — bloquear el alta entera por un problema de SMTP sería peor.
        FuncionarioResponseDTO response = funcionarioMapper.toDto(guardado);
        try {
            emailService.enviarContraseniaTemporal(guardado.getEmail(), guardado.getNombre(), contraseniaPlana);
        } catch (Exception e) {
            log.warn("[AUDIT] No se pudo enviar el email de contraseña temporal al funcionario {}: {}",
                    guardado.getIdUsuario(), e.getMessage());
            response.setAdvertencia("El funcionario se creó correctamente, pero no se pudo enviar el email " +
                    "con la contraseña temporal. Usá 'Restablecer contraseña' para reintentar el envío.");
        }

        return response;
    }

    @PreAuthorize("hasAuthority('VER_FUNCIONARIOS')")
    @Transactional(readOnly = true)
    public FuncionarioResponseDTO obtenerPorId(Integer id) {
        return funcionarioMapper.toDto(buscarOFallar(id));
    }

    @PreAuthorize("hasAuthority('VER_FUNCIONARIOS')")
    @Transactional(readOnly = true)
    public FuncionarioResponseDTO obtenerPorDocumento(String documento) {
        Funcionario funcionario = funcionarioRepository
                .findByDocumentoAndEstado(documento, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionario no encontrado"));
        return funcionarioMapper.toDto(funcionario);
    }

    @PreAuthorize("hasAuthority('VER_FUNCIONARIOS')")
    @Transactional(readOnly = true)
    public Page<FuncionarioResponseDTO> listar(String texto, EstadoLogico estado, Pageable pageable) {
        EstadoLogico estadoEfectivo = estado != null ? estado : EstadoLogico.ACTIVO;
        Page<Funcionario> pagina = texto != null
                ? funcionarioRepository.buscarPorTextoYEstado(texto, estadoEfectivo, pageable)
                : funcionarioRepository.findByEstado(estadoEfectivo, pageable);
        List<FuncionarioResponseDTO> dtos = funcionarioMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @PreAuthorize("hasAuthority('EDITAR_FUNCIONARIO')")
    @Transactional
    public FuncionarioResponseDTO editar(Integer id, FuncionarioUpdateDTO dto) {

        Funcionario funcionario = buscarOFallar(id);

        if (!funcionario.getEmail().equalsIgnoreCase(dto.getEmail())
                && funcionarioRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese email.");
        }

        aplicarCambios(funcionario, dto);
        return funcionarioMapper.toDto(funcionarioRepository.save(funcionario));
    }


    @PreAuthorize("hasAuthority('EDITAR_FUNCIONARIO')")
    @Transactional
    public void restablecerContrasenia(Integer id) {
        Funcionario funcionario = buscarOFallar(id);

        String contraseniaPlana = passwordGeneratorService.generar();
        funcionario.setContrasenia(passwordEncoder.encode(contraseniaPlana));
        funcionario.setCredencialesVigentesDesde(LocalDateTime.now());
        funcionarioRepository.save(funcionario);

        emailService.enviarContraseniaRestablecida(funcionario.getEmail(), funcionario.getNombre(), contraseniaPlana);
        log.info("[AUDIT] Admin restableció contraseña del funcionario id={}", id);
    }

    @PreAuthorize("hasAuthority('DESACTIVAR_FUNCIONARIO')")
    @Transactional
    public void desactivar(Integer id) {
        Funcionario funcionario = buscarOFallar(id);

        // Mismo criterio que en aplicarCambios(): sin esto, un actor con
        // DESACTIVAR_FUNCIONARIO pero permisos limitados podía desactivar a un
        // funcionario con más privilegios que él mismo (ej. al ADMINISTRADOR real).
        ValidadorDePermisosDeRol.validarActorPuedeAsignarRol(funcionario.getRol());

        if (funcionario.getEstado() == EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El funcionario ya se encuentra inactivo");
        }
        funcionario.setEstado(EstadoLogico.INACTIVO);
        funcionarioRepository.save(funcionario);
    }

    @PreAuthorize("hasAuthority('REACTIVAR_FUNCIONARIO')")
    @Transactional
    public FuncionarioResponseDTO reactivar(Integer id) {
        Funcionario funcionario = buscarOFallar(id);

        // Mismo criterio que en desactivar(): un actor no debería poder tocar el
        // estado de un funcionario con más privilegios que él mismo.
        ValidadorDePermisosDeRol.validarActorPuedeAsignarRol(funcionario.getRol());

        if (funcionario.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El funcionario ya se encuentra activo");
        }
        funcionario.setEstado(EstadoLogico.ACTIVO);
        funcionarioRepository.save(funcionario);
        return funcionarioMapper.toDto(funcionario);
    }

    private Funcionario buscarOFallar(Integer id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionario no encontrado: " + id));
    }

    private void aplicarCambios(Funcionario funcionario, FuncionarioUpdateDTO dto) {
        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado: " + dto.getIdRol()));

        // Sin esto, un actor con EDITAR_FUNCIONARIO pero permisos limitados podía
        // editar (incluido reasignarle el rol) a un funcionario con más privilegios
        // que él mismo — ej. degradar al ADMINISTRADOR real a un rol propio de bajo
        // privilegio. validarActorPuedeAsignarRol ya evitaba asignar un rol de
        // destino con permisos que el actor no tiene; acá se agrega la misma
        // validación contra el rol que el funcionario YA tenía.
        ValidadorDePermisosDeRol.validarActorPuedeAsignarRol(funcionario.getRol());
        ValidadorDePermisosDeRol.validarActorPuedeAsignarRol(rol);

        funcionario.setNombre(dto.getNombre());
        funcionario.setApellido(dto.getApellido());
        funcionario.setEmail(dto.getEmail());
        funcionario.setRol(rol);
    }

    private void validarDocumento(FuncionarioRequestDTO dto) {
        if (dto.getPaisDocumento() == PaisDocumento.URY
                && !ValidadorDeCedulasUy.esCedulaValida(dto.getDocumento())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cédula uruguaya es inválida.");
        }
    }
    // Exclusivo del propio funcionario: para plantarle una contraseña a otro sin que la
    // elija ni la sepa está restablecerContrasenia() (genera una random y la manda por
    // mail). Este endpoint es "yo elijo mi nueva contraseña", por eso exige la actual.
    @PreAuthorize("authentication.name == #id.toString()")
    @Transactional
    public void cambiarContrasenia(Integer id, CambiarContraseniaDTO dto) {
        Funcionario funcionario = buscarOFallar(id);

        if (!passwordEncoder.matches(dto.getContraseniaActual(), funcionario.getContrasenia())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contraseña actual no es correcta.");
        }

        funcionario.setContrasenia(passwordEncoder.encode(dto.getContrasenia()));
        funcionario.setCredencialesVigentesDesde(LocalDateTime.now());

        if (EstadoLogico.PENDIENTE_DE_ACTIVACION.equals(funcionario.getEstado())) {
            funcionario.setEstado(EstadoLogico.ACTIVO);
        }

        funcionario.setBloqueado(false);
        funcionarioRepository.save(funcionario);
        log.info("[AUDIT] Funcionario id={} cambió su contraseña", id);
    }
}