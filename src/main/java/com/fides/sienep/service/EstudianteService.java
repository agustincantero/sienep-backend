package com.fides.sienep.service;

import com.fides.sienep.dto.CambiarContraseniaDTO;
import com.fides.sienep.dto.EstudianteRequestDTO;
import com.fides.sienep.dto.EstudianteResponseDTO;
import com.fides.sienep.dto.EstudianteResumenDTO;
import com.fides.sienep.dto.EstudianteUpdateDTO;
import com.fides.sienep.dto.MisDatosRequestDTO;
import com.fides.sienep.mapper.EstudianteMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.GrupoRepository;
import com.fides.sienep.dto.ArchivoDescargaDTO;
import com.fides.sienep.utils.ValidadorDeCedulasUy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private static final List<String> EXTENSIONES_FOTO_PERMITIDAS = List.of("jpg", "jpeg", "png");
    private static final long MAX_TAMANIO_FOTO_BYTES = 5L * 1024 * 1024;
    private static final String SUBCARPETA_FOTOS = "fotos";

    private final FuncionarioRepository funcionarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final GrupoRepository grupoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EstudianteMapper estudianteMapper;
    private final EmailService emailService;
    private final PasswordGeneratorService passwordGeneratorService;
    private final FileStorageService fileStorageService;
    private static final Logger log = LoggerFactory.getLogger(EstudianteService.class);
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

        if (dto.getPaisDocumento() == PaisDocumento.URY
                && !ValidadorDeCedulasUy.esCedulaValida(dto.getDocumento())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cédula de identidad uruguaya ingresada no es válida.");
        }

        validarEdad(dto.getFechaNacimiento());

        String contraseniaPlana = passwordGeneratorService.generar();

        Estudiante estudiante = new Estudiante(
                dto.getNombre(), dto.getApellido(), dto.getDocumento(),
                dto.getPaisDocumento(), passwordEncoder.encode(contraseniaPlana),
                dto.getEmail(), dto.getCiudad(), dto.getDepartamento(),
                dto.getCalle(), dto.getNroPuerta(), dto.getInformacionSalud(),
                dto.getSistemaSalud(), dto.getFechaNacimiento(),
                dto.getMotivoDerivacion(), null, null, null
        );

        asignarTelefonos(estudiante, dto.getTelefonos());
        asignarGrupos(estudiante, dto.getIdGrupos());

        Estudiante guardado = estudianteRepository.save(estudiante);

        // RF05 — un fallo al enviar el email no debe impedir el alta: los datos ya son
        // válidos y persistidos, y existe un camino de recuperación (resetContrasenia).
        // A diferencia de resetContrasenia(), acá no bloqueamos porque la cuenta recién
        // creada no tiene ninguna contraseña previa que se pueda seguir usando mientras
        // tanto — bloquear el alta entera por un problema de SMTP sería peor.
        EstudianteResponseDTO response = estudianteMapper.toDto(guardado);
        try {
            emailService.enviarContraseniaTemporal(guardado.getEmail(), guardado.getNombre(), contraseniaPlana);
        } catch (Exception e) {
            log.warn("[AUDIT] No se pudo enviar el email de contraseña temporal al estudiante {}: {}",
                    guardado.getIdUsuario(), e.getMessage());
            response.setAdvertencia("El estudiante se creó correctamente, pero no se pudo enviar el email " +
                    "con la contraseña temporal. Usá 'Resetear contraseña' para reintentar el envío.");
        }

        return response;
    }

    // =========================================================
    // CONSULTAS
    // =========================================================

    @PreAuthorize("hasAuthority('VER_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public EstudianteResponseDTO obtenerPorId(Integer id) {
        return estudianteMapper.toDto(buscarOFallar(id));
    }

    @PreAuthorize("hasAuthority('VER_ESTUDIANTE_RESUMIDO')")
    @Transactional(readOnly = true)
    public EstudianteResumenDTO obtenerResumenPorId(Integer id) {
        return estudianteMapper.toResumenDto(buscarOFallar(id));
    }

    @PreAuthorize("hasAuthority('VER_ESTUDIANTE_RESUMIDO')")
    @Transactional(readOnly = true)
    public Page<EstudianteResumenDTO> listar(String texto, Integer idGrupo, Integer idCarrera, Integer idItr, EstadoLogico estado, Pageable pageable) {
        boolean aplicaFiltro = texto != null || idGrupo != null || idCarrera != null || idItr != null;
        if (aplicaFiltro && !tienePermiso("BUSCAR_ESTUDIANTE")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requiere permiso BUSCAR_ESTUDIANTE para buscar o filtrar.");
        }

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
            resultado = estadoEfectivo != null
                    ? estudianteRepository.findByEstado(estadoEfectivo, pageable)
                    : estudianteRepository.findAll(pageable);
        }

        return resultado.map(estudianteMapper::toResumenDto);
    }

    @PreAuthorize("hasAuthority('EDITAR_ESTUDIANTE')")
    @Transactional
    public EstudianteResponseDTO actualizar(Integer id, EstudianteUpdateDTO dto) {
        Estudiante estudiante = buscarOFallar(id);
        aplicarCambios(estudiante, dto);
        return estudianteMapper.toDto(estudianteRepository.save(estudiante));
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional
    public EstudianteResponseDTO editarMisDatos(Integer id, MisDatosRequestDTO dto) {
        if (!getIdUsuarioAutenticado().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo podés editar tus propios datos.");
        }
        Estudiante estudiante = buscarOFallar(id);
        aplicarCambiosContacto(estudiante, dto);
        return estudianteMapper.toDto(estudianteRepository.save(estudiante));
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public EstudianteResumenDTO verMiPerfil() {
        Integer id = getIdUsuarioAutenticado();
        return estudianteMapper.toResumenDto(buscarOFallar(id));
    }

    // =========================================================
    // CAMBIAR CONTRASEÑA
    // =========================================================
    @PreAuthorize("authentication.name == #id.toString() or hasAuthority('EDITAR_ESTUDIANTE')")
    @Transactional
    public void cambiarContrasenia(Integer id, CambiarContraseniaDTO dto) {
        Estudiante estudiante = buscarOFallar(id);
        estudiante.setContrasenia(passwordEncoder.encode(dto.getContrasenia()));
        if (EstadoLogico.PENDIENTE_DE_ACTIVACION.equals(estudiante.getEstado())) {
            estudiante.setEstado(EstadoLogico.ACTIVO);
        }
        estudiante.setBloqueado(false);
        estudianteRepository.save(estudiante);
        log.info("[AUDIT] Estudiante id={} cambió su contraseña", id);
    }


    @PreAuthorize("hasAuthority('EDITAR_ESTUDIANTE')")
    @Transactional
    public void resetContrasenia(Integer id) {
        Estudiante estudiante = buscarOFallar(id);

        String contraseniaPlana = passwordGeneratorService.generar();
        estudiante.setContrasenia(passwordEncoder.encode(contraseniaPlana));
        estudianteRepository.save(estudiante);

        emailService.enviarContraseniaRestablecida(estudiante.getEmail(), estudiante.getNombre(), contraseniaPlana);
        log.info("[AUDIT] Admin reseteó contraseña del estudiante id={}", id);
    }

    // =========================================================
    // FOTO DE PERFIL
    // =========================================================

    @PreAuthorize("authentication.name == #id.toString() or hasAuthority('EDITAR_ESTUDIANTE')")
    @Transactional
    public EstudianteResponseDTO subirFoto(Integer id, MultipartFile foto) {
        Estudiante estudiante = buscarOFallar(id);

        String subdirectorio = SUBCARPETA_FOTOS + "/" + id;
        String nombreFisico = fileStorageService.guardar(
                foto, subdirectorio, EXTENSIONES_FOTO_PERMITIDAS, MAX_TAMANIO_FOTO_BYTES);

        // No se borra el archivo físico anterior (si existía): mismo criterio de
        // "nunca borrado físico" que se usa en el resto del sistema (baja lógica).
        estudiante.setFoto(subdirectorio + "/" + nombreFisico);

        return estudianteMapper.toDto(estudianteRepository.save(estudiante));
    }

    @PreAuthorize("authentication.name == #id.toString() or hasAuthority('VER_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public ArchivoDescargaDTO obtenerFoto(Integer id) {
        Estudiante estudiante = buscarOFallar(id);
        if (estudiante.getFoto() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El estudiante no tiene foto cargada");
        }

        Resource recurso = fileStorageService.cargar(estudiante.getFoto());
        String extension = fileStorageService.extraerExtension(estudiante.getFoto());
        MediaType contentType = fileStorageService.mediaTypeDeExtension(extension);
        String nombreDescarga = "foto_" + id + "." + extension;

        return new ArchivoDescargaDTO(recurso, contentType, nombreDescarga);
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

    private void validarEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) return;
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estudiante debe ser mayor de edad (mínimo 18 años).");
        }
        if (fechaNacimiento.isBefore(LocalDate.now().minusYears(100))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de nacimiento no puede ser mayor a 100 años.");
        }
    }

    private void aplicarCambios(Estudiante estudiante, EstudianteUpdateDTO dto) {
        validarEdad(dto.getFechaNacimiento());
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

    private boolean tienePermiso(String permiso) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        for (var authority : auth.getAuthorities()) {
            if (authority.getAuthority().equals(permiso)) return true;
        }
        return false;
    }

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(auth.getName());
    }
}