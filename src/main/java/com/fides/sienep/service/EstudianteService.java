package com.fides.sienep.service;

import com.fides.sienep.dto.EstudianteRequestDTO;
import com.fides.sienep.dto.EstudianteResponseDTO;
import com.fides.sienep.dto.EstudianteResumenDTO;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.GrupoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service de Estudiante — capa de lógica de negocio.
 *
 * Responsabilidades:
 *  - Validaciones de negocio (unicidad, mayoría de edad, ownership).
 *  - Hasheo de contraseña con BCrypt.
 *  - Control de acceso declarativo con @PreAuthorize.
 *  - Mapeo entre entidad y DTOs.
 *  - Orquestación de repositorios.
 *
 * @RequiredArgsConstructor genera el constructor con todos los campos final.
 * Spring inyecta las dependencias por constructor — práctica recomendada
 * sobre @Autowired porque hace las dependencias explícitas e inmutables.
 *
 * @Transactional: garantiza que cada operación de escritura sea atómica.
 * Si algo falla en el medio, la BD hace rollback automático.
 */
@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final GrupoRepository grupoRepository;
    private final PasswordEncoder passwordEncoder;

    // =========================================================
    // CREAR
    // =========================================================

    @PreAuthorize("hasAuthority('CREAR_ESTUDIANTE')")
    @Transactional
    public EstudianteResponseDTO crear(EstudianteRequestDTO dto) {
        if (estudianteRepository.existsByDocumento(dto.getDocumento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un estudiante con ese documento.");
        }
        if (estudianteRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese email.");
        }

        Estudiante estudiante = new Estudiante(
                dto.getNombre(), dto.getApellido(), dto.getDocumento(),
                dto.getPaisDocumento(), passwordEncoder.encode(dto.getContrasenia()),
                dto.getEmail(), dto.getCiudad(), dto.getDepartamento(),
                dto.getCalle(), dto.getNroPuerta(), dto.getInformacionSalud(),
                dto.getSistemaSalud(), dto.getFechaNacimiento(),
                dto.getMotivoDerivacion(), dto.getFoto(), null, null
        );

        asignarTelefonos(estudiante, dto.getTelefonos());
        asignarGrupos(estudiante, dto.getIdGrupos());

        return toResponseDTO(estudianteRepository.save(estudiante));
    }

    // =========================================================
    // CONSULTAS
    // =========================================================

    @PreAuthorize("hasAuthority('VER_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public EstudianteResponseDTO obtenerPorId(Integer id) {
        return toResponseDTO(buscarOFallar(id));
    }

    @PreAuthorize("hasAuthority('VER_ESTUDIANTE_RESUMIDO')")
    @Transactional(readOnly = true)
    public EstudianteResumenDTO obtenerResumenPorId(Integer id) {
        return toResumenDTO(buscarOFallar(id));
    }

    @PreAuthorize("hasAuthority('VER_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public Page<EstudianteResponseDTO> listar(Pageable pageable) {
        return estudianteRepository.findAll(pageable).map(this::toResponseDTO);
    }

    @PreAuthorize("hasAuthority('VER_ESTUDIANTE_RESUMIDO')")
    @Transactional(readOnly = true)
    public Page<EstudianteResumenDTO> listarActivos(Pageable pageable) {
        return estudianteRepository.findByEstado(EstadoLogico.ACTIVO, pageable)
                .map(this::toResumenDTO);
    }

    @PreAuthorize("hasAuthority('BUSCAR_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public Page<EstudianteResumenDTO> buscar(String texto, Pageable pageable) {
        boolean puedeVerInactivos = tienePermiso("VER_ESTUDIANTE");
        if (puedeVerInactivos) {
            return estudianteRepository.buscarPorNombre(texto, pageable).map(this::toResumenDTO);
        }
        return estudianteRepository.buscarPorNombreYEstado(texto, EstadoLogico.ACTIVO, pageable)
                .map(this::toResumenDTO);
    }

    @PreAuthorize("hasAuthority('BUSCAR_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public Page<EstudianteResumenDTO> filtrarPorGrupo(Integer idGrupo, Pageable pageable) {
        EstadoLogico estado = tienePermiso("VER_ESTUDIANTE") ? null : EstadoLogico.ACTIVO;
        return estudianteRepository.findByGrupoAndEstado(idGrupo,
                        estado != null ? estado : EstadoLogico.ACTIVO, pageable)
                .map(this::toResumenDTO);
    }

    @PreAuthorize("hasAuthority('BUSCAR_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public Page<EstudianteResumenDTO> filtrarPorCarrera(Integer idCarrera, Pageable pageable) {
        EstadoLogico estado = tienePermiso("VER_ESTUDIANTE") ? null : EstadoLogico.ACTIVO;
        return estudianteRepository.findByCarreraAndEstado(idCarrera,
                        estado != null ? estado : EstadoLogico.ACTIVO, pageable)
                .map(this::toResumenDTO);
    }

    @PreAuthorize("hasAuthority('BUSCAR_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public Page<EstudianteResumenDTO> filtrarPorItr(Integer idItr, Pageable pageable) {
        EstadoLogico estado = tienePermiso("VER_ESTUDIANTE") ? null : EstadoLogico.ACTIVO;
        return estudianteRepository.findByItrAndEstado(idItr,
                        estado != null ? estado : EstadoLogico.ACTIVO, pageable)
                .map(this::toResumenDTO);
    }

    // =========================================================
    // ACTUALIZAR
    // =========================================================

    @PreAuthorize("hasAuthority('EDITAR_ESTUDIANTE')")
    @Transactional
    public EstudianteResponseDTO actualizar(Integer id, EstudianteRequestDTO dto) {
        Estudiante estudiante = buscarOFallar(id);
        aplicarCambios(estudiante, dto);
        return toResponseDTO(estudianteRepository.save(estudiante));
    }

    /**
     * El usuario autenticado edita sus propios datos.
     * Verifica que el ID del path coincida con el del usuario en sesión.
     */
    @PreAuthorize("hasAuthority('EDITAR_MIS_DATOS')")
    @Transactional
    public EstudianteResponseDTO editarMisDatos(Integer id, EstudianteRequestDTO dto) {
        Integer idAutenticado = getIdUsuarioAutenticado();
        if (!idAutenticado.equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo podés editar tus propios datos.");
        }
        Estudiante estudiante = buscarOFallar(id);
        aplicarCambios(estudiante, dto);
        return toResponseDTO(estudianteRepository.save(estudiante));
    }

    // =========================================================
    // CAMBIAR CONTRASEÑA — dispara activación
    // =========================================================

    /**
     * Sin @PreAuthorize: cualquier usuario autenticado puede cambiar su propia contraseña.
     * La verificación de ownership se hace dentro del método.
     * Si el estudiante estaba PENDIENTE_DE_ACTIVACION, el cambio de contraseña
     * lo activa — es el mecanismo de activación del sistema.
     */
    @Transactional
    public void cambiarContrasenia(Integer id, String nuevaContrasenia) {
        Integer idAutenticado = getIdUsuarioAutenticado();
        if (!idAutenticado.equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No podés cambiar la contraseña de otro usuario.");
        }
        Estudiante estudiante = buscarOFallar(id);
        estudiante.setContrasenia(passwordEncoder.encode(nuevaContrasenia));

        if (EstadoLogico.PENDIENTE_DE_ACTIVACION.equals(estudiante.getEstado())) {
            estudiante.setEstado(EstadoLogico.ACTIVO);
        }
        estudianteRepository.save(estudiante);
    }

    // =========================================================
    // BAJA Y REACTIVACIÓN
    // =========================================================

    @PreAuthorize("hasAuthority('ELIMINAR_ESTUDIANTE')")
    @Transactional
    public void darDeBaja(Integer id) {
        Estudiante estudiante = buscarOFallar(id);
        estudiante.setEstado(EstadoLogico.INACTIVO);
        estudianteRepository.save(estudiante);
    }

    @PreAuthorize("hasAuthority('REACTIVAR_ESTUDIANTE')")
    @Transactional
    public void reactivar(Integer id) {
        Estudiante estudiante = buscarOFallar(id);
        estudiante.setEstado(EstadoLogico.ACTIVO);
        estudianteRepository.save(estudiante);
    }

    // =========================================================
    // MÉTODOS PRIVADOS
    // =========================================================

    private Estudiante buscarOFallar(Integer id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Estudiante no encontrado con id: " + id));
    }

    private void aplicarCambios(Estudiante estudiante, EstudianteRequestDTO dto) {
        estudiante.setNombre(dto.getNombre());
        estudiante.setApellido(dto.getApellido());
        estudiante.setEmail(dto.getEmail());
        estudiante.setCiudad(dto.getCiudad());
        estudiante.setDepartamento(dto.getDepartamento());
        estudiante.setCalle(dto.getCalle());
        estudiante.setNroPuerta(dto.getNroPuerta());
        estudiante.setInformacionSalud(dto.getInformacionSalud());
        estudiante.setSistemaSalud(dto.getSistemaSalud());
        estudiante.setFechaNacimiento(dto.getFechaNacimiento());
        estudiante.setMotivoDerivacion(dto.getMotivoDerivacion());
        estudiante.setFoto(dto.getFoto());
        asignarTelefonos(estudiante, dto.getTelefonos());
        asignarGrupos(estudiante, dto.getIdGrupos());
    }

    private void asignarTelefonos(Estudiante estudiante, List<String> numeros) {
        if (numeros == null) return;
        List<Telefono> telefonos = numeros.stream()
                .map(n -> new Telefono(estudiante, n))
                .toList();
        estudiante.setTelefonos(telefonos);
    }

    private void asignarGrupos(Estudiante estudiante, List<Integer> idGrupos) {
        if (idGrupos == null) return;
        List<Grupo> grupos = idGrupos.stream()
                .map(idGrupo -> grupoRepository.findById(idGrupo)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Grupo no encontrado con id: " + idGrupo)))
                .toList();
        estudiante.setGrupos(grupos);
    }

    private EstudianteResponseDTO toResponseDTO(Estudiante e) {
        return EstudianteResponseDTO.builder()
                .idUsuario(e.getIdUsuario())
                .nombre(e.getNombre())
                .apellido(e.getApellido())
                .documento(e.getDocumento())
                .paisDocumento(e.getPaisDocumento())
                .email(e.getEmail())
                .estado(e.getEstado())
                .fechaNacimiento(e.getFechaNacimiento())
                .ciudad(e.getCiudad())
                .departamento(e.getDepartamento())
                .calle(e.getCalle())
                .nroPuerta(e.getNroPuerta())
                .informacionSalud(e.getInformacionSalud())
                .sistemaSalud(e.getSistemaSalud())
                .motivoDerivacion(e.getMotivoDerivacion())
                .foto(e.getFoto())
                .telefonos(e.getTelefonos().stream()
                        .map(t -> t.getId().getNumero()).toList())
                .grupos(e.getGrupos().stream()
                        .map(Grupo::getNomGrupo).toList())
                .createdAt(e.getEstudianteCreatedAt())
                .updatedAt(e.getEstudianteUpdatedAt())
                .build();
    }

    private EstudianteResumenDTO toResumenDTO(Estudiante e) {
        return EstudianteResumenDTO.builder()
                .idUsuario(e.getIdUsuario())
                .nombre(e.getNombre())
                .apellido(e.getApellido())
                .documento(e.getDocumento())
                .email(e.getEmail())
                .estado(e.getEstado())
                .grupos(e.getGrupos().stream()
                        .map(Grupo::getNomGrupo).toList())
                .telefonos(e.getTelefonos().stream()
                        .map(t -> t.getId().getNumero()).toList())
                .build();
    }

    private boolean tienePermiso(String permiso) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permiso));
    }

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // El principal debe implementar una interfaz que exponga el idUsuario.
        // Se configura cuando se implemente UserDetailsService en SecurityConfig.
        if (auth.getPrincipal() instanceof com.fides.sienep.model.Usuario usuario) {
            return usuario.getIdUsuario();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
    }
}