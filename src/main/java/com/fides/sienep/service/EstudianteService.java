package com.fides.sienep.service;

import com.fides.sienep.dto.CambiarContraseniaDTO;
import com.fides.sienep.dto.EstudianteRequestDTO;
import com.fides.sienep.dto.EstudianteResponseDTO;
import com.fides.sienep.dto.EstudianteResumenDTO;
import com.fides.sienep.dto.MisDatosRequestDTO;
import com.fides.sienep.model.EstadoLogico;
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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final GrupoRepository grupoRepository;
    // TODO: Descomentar cuando la compañera suba su rama con SecurityConfig
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
                dto.getPaisDocumento(), dto.getContrasenia(), /* TODO: usar passwordEncoder.encode() */
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

    /**
     * Método unificado para listar, buscar y filtrar.
     * Corresponde al endpoint GET /estudiantes con query params opcionales.
     *
     * Lógica de permisos:
     *   - Si hay texto, grupo, carrera o itr → requiere BUSCAR_ESTUDIANTE (verificado dentro).
     *   - Roles sin VER_ESTUDIANTE siempre ven solo ACTIVO, aunque pasen ?estado=INACTIVO.
     *   - Siempre devuelve resumido — los datos completos son por GET /{id}.
     */
    @PreAuthorize("hasAuthority('VER_ESTUDIANTE_RESUMIDO')")
    @Transactional(readOnly = true)
    public Page<EstudianteResumenDTO> listar(String texto, Integer idGrupo, Integer idCarrera,
                                             Integer idItr, EstadoLogico estado, Pageable pageable) {
        boolean aplicaFiltro = texto != null || idGrupo != null || idCarrera != null || idItr != null;
        if (aplicaFiltro && !tienePermiso("BUSCAR_ESTUDIANTE")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requiere permiso BUSCAR_ESTUDIANTE para buscar o filtrar.");
        }

        // Roles sin VER_ESTUDIANTE solo ven activos
        EstadoLogico estadoEfectivo = tienePermiso("VER_ESTUDIANTE") ? estado : EstadoLogico.ACTIVO;

        Page<Estudiante> resultado;
        if (texto != null) {
            resultado = estadoEfectivo != null
                    ? estudianteRepository.buscarPorNombreYEstado(texto, estadoEfectivo, pageable)
                    : estudianteRepository.buscarPorNombre(texto, pageable);
        } else if (idGrupo != null) {
            resultado = estadoEfectivo != null
                    ? estudianteRepository.findByGrupoAndEstado(idGrupo, estadoEfectivo, pageable)
                    : estudianteRepository.findByGrupo(idGrupo, pageable);
        } else if (idCarrera != null) {
            resultado = estadoEfectivo != null
                    ? estudianteRepository.findByCarreraAndEstado(idCarrera, estadoEfectivo, pageable)
                    : estudianteRepository.findByCarrera(idCarrera, pageable);
        } else if (idItr != null) {
            resultado = estadoEfectivo != null
                    ? estudianteRepository.findByItrAndEstado(idItr, estadoEfectivo, pageable)
                    : estudianteRepository.findByItr(idItr, pageable);
        } else {
            // Sin filtros: lista paginada
            resultado = estadoEfectivo != null
                    ? estudianteRepository.findByEstado(estadoEfectivo, pageable)
                    : estudianteRepository.findAll(pageable);
        }

        Page<EstudianteResumenDTO> page = resultado.map(this::toResumenDTO);
        return page;
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
     * Autoservicio del estudiante. Gateado por ROLE_ESTUDIANTE (authority
     * sintética asignada en autenticación, no por el RBAC de funcionarios).
     * Solo edita datos de contacto y solo sobre la cuenta propia.
     */
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional
    public EstudianteResponseDTO editarMisDatos(Integer id, MisDatosRequestDTO dto) {
        if (!getIdUsuarioAutenticado().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo podés editar tus propios datos.");
        }
        Estudiante estudiante = buscarOFallar(id);
        aplicarCambiosContacto(estudiante, dto);
        return toResponseDTO(estudianteRepository.save(estudiante));
    }

    // =========================================================
    // CAMBIAR CONTRASEÑA — dispara activación
    // =========================================================

    @Transactional
    public void cambiarContrasenia(Integer id, CambiarContraseniaDTO dto) {
        if (!getIdUsuarioAutenticado().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No podés cambiar la contraseña de otro usuario.");
        }
        Estudiante estudiante = buscarOFallar(id);
        estudiante.setContrasenia(dto.getContrasenia()); /* TODO: passwordEncoder.encode() */
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado: " + id));
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

    private void aplicarCambiosContacto(Estudiante estudiante, MisDatosRequestDTO dto) {
        estudiante.setCiudad(dto.getCiudad());
        estudiante.setDepartamento(dto.getDepartamento());
        estudiante.setCalle(dto.getCalle());
        estudiante.setNroPuerta(dto.getNroPuerta());
        asignarTelefonos(estudiante, dto.getTelefonos());
    }

    private void asignarTelefonos(Estudiante estudiante, List<String> numeros) {
        if (numeros == null) return;
        List<Telefono> telefonos = new ArrayList<>();
        for (String numero : numeros) {
            telefonos.add(new Telefono(estudiante, numero));
        }
        estudiante.setTelefonos(telefonos);
    }

    private void asignarGrupos(Estudiante estudiante, List<Integer> idGrupos) {
        if (idGrupos == null) return;
        List<Grupo> grupos = new ArrayList<>();
        for (Integer idGrupo : idGrupos) {
            Grupo grupo = grupoRepository.findById(idGrupo)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo no encontrado: " + idGrupo));
            grupos.add(grupo);
        }
        estudiante.setGrupos(grupos);
    }

    private EstudianteResponseDTO toResponseDTO(Estudiante e) {
        List<String> telefonos = new ArrayList<>();
        for (Telefono t : e.getTelefonos()) {
            telefonos.add(t.getId().getNumero());
        }
        List<String> grupos = new ArrayList<>();
        for (Grupo g : e.getGrupos()) {
            grupos.add(g.getNomGrupo());
        }
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
                .telefonos(telefonos)
                .grupos(grupos)
                .createdAt(e.getEstudianteCreatedAt())
                .updatedAt(e.getEstudianteUpdatedAt())
                .build();
    }

    private EstudianteResumenDTO toResumenDTO(Estudiante e) {
        List<String> telefonos = new ArrayList<>();
        for (Telefono t : e.getTelefonos()) {
            telefonos.add(t.getId().getNumero());
        }
        List<String> grupos = new ArrayList<>();
        for (Grupo g : e.getGrupos()) {
            grupos.add(g.getNomGrupo());
        }
        return EstudianteResumenDTO.builder()
                .idUsuario(e.getIdUsuario())
                .nombre(e.getNombre())
                .apellido(e.getApellido())
                .documento(e.getDocumento())
                .email(e.getEmail())
                .estado(e.getEstado())
                .grupos(grupos)
                .telefonos(telefonos)
                .build();
    }

    private boolean tienePermiso(String permiso) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        for (var authority : auth.getAuthorities()) {
            if (authority.getAuthority().equals(permiso)) return true;
        }
        return false;
    }

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof Usuario usuario) {
            return usuario.getIdUsuario();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado.");
    }
}