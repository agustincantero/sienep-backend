package com.fides.sienep.service;

import com.fides.sienep.dto.CambiarContraseniaDTO;
import com.fides.sienep.dto.EstudianteRequestDTO;
import com.fides.sienep.dto.EstudianteResponseDTO;
import com.fides.sienep.dto.EstudianteResumenDTO;
import com.fides.sienep.dto.EstudianteUpdateDTO;
import com.fides.sienep.dto.MiPerfilEstudianteDTO;
import com.fides.sienep.dto.MisDatosRequestDTO;
import com.fides.sienep.dto.ProcesosAbiertosDTO;
import com.fides.sienep.mapper.EstudianteMapper;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.EstudianteSpecs;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.GrupoRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.repository.RecordatorioRepository;
import com.fides.sienep.repository.RolRepository;
import com.fides.sienep.dto.ArchivoDescargaDTO;
import com.fides.sienep.utils.ValidadorDeCedulasUy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
import java.time.LocalDateTime;
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
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final EstudianteMapper estudianteMapper;
    private final EmailService emailService;
    private final PasswordGeneratorService passwordGeneratorService;
    private final FileStorageService fileStorageService;
    private final InstanciaComunRepository instanciaComunRepository;
    private final RecordatorioRepository recordatorioRepository;
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

        Rol rolEstudiante = rolRepository.findByNombre("ESTUDIANTE")
                .orElseThrow(() -> new IllegalStateException(
                        "Rol ESTUDIANTE no configurado en la base de datos"));
        estudiante.setRol(rolEstudiante);

        Estudiante guardado = estudianteRepository.save(estudiante);

        // RF05 — un fallo al enviar el email no debe impedir el alta: los datos ya son
        // válidos y persistidos, y existe un camino de recuperación (restablecerContrasenia).
        // A diferencia de restablecerContrasenia(), acá no bloqueamos porque la cuenta recién
        // creada no tiene ninguna contraseña previa que se pueda seguir usando mientras
        // tanto — bloquear el alta entera por un problema de SMTP sería peor.
        EstudianteResponseDTO response = estudianteMapper.toDto(guardado);
        try {
            emailService.enviarContraseniaTemporal(guardado.getEmail(), guardado.getNombre(), contraseniaPlana);
        } catch (Exception e) {
            log.warn("[AUDIT] No se pudo enviar el email de contraseña temporal al estudiante {}: {}",
                    guardado.getIdUsuario(), e.getMessage());
            response.setAdvertencia("El estudiante se creó correctamente, pero no se pudo enviar el email " +
                    "con la contraseña temporal. Usa 'Restablecer contraseña' para reintentar el envío.");
        }

        return response;
    }

    // =========================================================
    // CONSULTAS
    // =========================================================

    // El bloque de salud/motivo de derivación requiere además VER_BLOQUE_CONFIDENCIAL
    // (asignado junto con VER_ESTUDIANTE a PSICOPEDAGOGO y LEGALES). Se nulifica
    // acá en vez de en el mapper para no acoplar MapStruct a SecurityContextHolder.
    @PreAuthorize("hasAuthority('VER_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public EstudianteResponseDTO obtenerPorId(Integer id) {
        return ocultarBloqueConfidencialSinPermiso(estudianteMapper.toDto(buscarOFallar(id)));
    }

    @PreAuthorize("hasAuthority('VER_ESTUDIANTE_RESUMIDO')")
    @Transactional(readOnly = true)
    public EstudianteResumenDTO obtenerResumenPorId(Integer id) {
        return aResumen(buscarOFallar(id), tienePermiso("VER_ESTUDIANTE"));
    }

    @PreAuthorize("hasAuthority('VER_ESTUDIANTE_RESUMIDO')")
    @Transactional(readOnly = true)
    public Page<EstudianteResumenDTO> listar(String texto, Integer idGrupo, Integer idCarrera, Integer idItr, EstadoLogico estado, Pageable pageable) {
        boolean aplicaFiltro = texto != null || idGrupo != null || idCarrera != null || idItr != null;
        if (aplicaFiltro && !tienePermiso("BUSCAR_ESTUDIANTE")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requiere permiso BUSCAR_ESTUDIANTE para buscar o filtrar.");
        }

        boolean puedeVerEstudiante = tienePermiso("VER_ESTUDIANTE");
        EstadoLogico estadoEfectivo = puedeVerEstudiante ? estado : EstadoLogico.ACTIVO;

        // Todos los filtros que lleguen se combinan (AND). Antes se aplicaba solo el primero de texto/grupo/carrera/ITR
        // y el resto se descartaba sin avisar: "Ana" + carrera LTI devolvía todas las "Ana" de cualquier carrera.
        Specification<Estudiante> spec = Specification.unrestricted();
        if (texto != null) spec = spec.and(EstudianteSpecs.texto(texto));
        if (idGrupo != null) spec = spec.and(EstudianteSpecs.grupo(idGrupo));
        if (idCarrera != null) spec = spec.and(EstudianteSpecs.carrera(idCarrera));
        if (idItr != null) spec = spec.and(EstudianteSpecs.itr(idItr));
        if (estadoEfectivo != null) spec = spec.and(EstudianteSpecs.estado(estadoEfectivo));

        Page<Estudiante> resultado = estudianteRepository.findAll(spec, pageable);

        return resultado.map(estudiante -> aResumen(estudiante, puedeVerEstudiante));
    }

    @PreAuthorize("hasAuthority('EDITAR_ESTUDIANTE')")
    @Transactional
    public EstudianteResponseDTO editar(Integer id, EstudianteUpdateDTO dto) {
        Estudiante estudiante = buscarOFallar(id);

        if (estudianteRepository.existsByEmailAndIdUsuarioNot(dto.getEmail(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese email.");
        }

        aplicarCambios(estudiante, dto);
        return estudianteMapper.toDto(estudianteRepository.save(estudiante));
    }

    // Devuelve MiPerfilEstudianteDTO (no EstudianteResponseDTO): la ficha completa incluye el bloque
    // confidencial y el motivo de derivación, que no se exponen por el autoservicio.
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional
    public MiPerfilEstudianteDTO editarMisDatos(Integer id, MisDatosRequestDTO dto) {
        if (!getIdUsuarioAutenticado().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes editar tus propios datos.");
        }
        Estudiante estudiante = buscarOFallar(id);
        aplicarCambiosContacto(estudiante, dto);
        return estudianteMapper.toMiPerfilDto(estudianteRepository.save(estudiante));
    }

    // Incluye la dirección (a diferencia del resumen): mis-datos es un PUT de reemplazo completo, así
    // que el estudiante necesita leer su dirección actual para poder editar solo una parte.
    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public MiPerfilEstudianteDTO verMiPerfil() {
        return estudianteMapper.toMiPerfilDto(buscarOFallar(getIdUsuarioAutenticado()));
    }

    // La ruta de la foto solo se ofrece a quien puede pedirla: GET /estudiantes/{id}/foto exige
    // VER_ESTUDIANTE o ser el propio estudiante (ver obtenerFoto). El resumen también lo ven roles
    // sin ese permiso (TUTOR, DOCENTE), a los que no se les da una URL que devolvería 403. Se
    // nulifica acá, igual que el bloque confidencial de obtenerPorId, para no acoplar MapStruct a
    // SecurityContextHolder.
    private EstudianteResumenDTO aResumen(Estudiante estudiante, boolean puedeVerFoto) {
        EstudianteResumenDTO dto = estudianteMapper.toResumenDto(estudiante);
        if (!puedeVerFoto) {
            dto.setUrlFoto(null);
        }
        return dto;
    }

    // =========================================================
    // CAMBIAR CONTRASEÑA
    // =========================================================
    // Exclusivo del propio estudiante: para plantarle una contraseña a otro sin que la
    // elija ni la sepa está restablecerContrasenia() (genera una random y la manda por
    // mail). Este endpoint es "yo elijo mi nueva contraseña", por eso exige la actual.
    @PreAuthorize("authentication.name == #id.toString()")
    @Transactional
    public void cambiarContrasenia(Integer id, CambiarContraseniaDTO dto) {
        Estudiante estudiante = buscarOFallar(id);

        if (!passwordEncoder.matches(dto.getContraseniaActual(), estudiante.getContrasenia())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contraseña actual no es correcta.");
        }

        estudiante.setContrasenia(passwordEncoder.encode(dto.getContrasenia()));
        estudiante.setCredencialesVigentesDesde(LocalDateTime.now());
        if (EstadoLogico.PENDIENTE_DE_ACTIVACION.equals(estudiante.getEstado())) {
            estudiante.setEstado(EstadoLogico.ACTIVO);
        }
        estudiante.setBloqueado(false);
        estudianteRepository.save(estudiante);
        log.info("[AUDIT] Estudiante id={} cambió su contraseña", id);
    }


    @PreAuthorize("hasAuthority('EDITAR_ESTUDIANTE')")
    @Transactional
    public void restablecerContrasenia(Integer id) {
        Estudiante estudiante = buscarOFallar(id);

        // Mismo criterio que CustomUsuarioDetailsService para decidir si una cuenta es
        // usable: sin este guard, resetear la contraseña de un estudiante INACTIVO o
        // ELIMINADO lo reactivaría de forma silenciosa (quedaría PENDIENTE_DE_ACTIVACION,
        // un estado logueable) sin pasar por reactivar().
        if (estudiante.getEstado() != EstadoLogico.ACTIVO
                && estudiante.getEstado() != EstadoLogico.PENDIENTE_DE_ACTIVACION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede restablecer la contraseña de un estudiante inactivo.");
        }

        String contraseniaPlana = passwordGeneratorService.generar();
        estudiante.setContrasenia(passwordEncoder.encode(contraseniaPlana));
        estudiante.setCredencialesVigentesDesde(LocalDateTime.now());
        // Deja al estudiante obligado a elegir una contraseña propia en el próximo
        // login (JwtRequestFilter lo restringe mientras esté en este estado) — sin esto,
        // si la cuenta ya estaba ACTIVO, el reset admin no dispara el cambio forzado.
        estudiante.setEstado(EstadoLogico.PENDIENTE_DE_ACTIVACION);
        estudianteRepository.save(estudiante);

        emailService.enviarContraseniaRestablecida(estudiante.getEmail(), estudiante.getNombre(), contraseniaPlana);
        log.info("[AUDIT] Admin restableció contraseña del estudiante id={}", id);
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
        String rutaAnterior = estudiante.getFoto();
        String rutaNueva = subdirectorio + "/" + nombreFisico;
        estudiante.setFoto(rutaNueva);

        try {
            Estudiante guardado = estudianteRepository.save(estudiante);
            // Lo puede llamar el propio estudiante (o un funcionario sin VER_BLOQUE_CONFIDENCIAL):
            // la respuesta no puede traer el bloque confidencial que obtenerPorId() le ocultaría.
            return ocultarBloqueConfidencialSinPermiso(estudianteMapper.toDto(guardado));
        } catch (RuntimeException e) {
            estudiante.setFoto(rutaAnterior);
            fileStorageService.eliminarSiExiste(rutaNueva);
            throw e;
        }
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

    // RF06 — "verificación de inexistencia de procesos críticos abiertos" antes de la baja.
    @PreAuthorize("hasAuthority('DESACTIVAR_ESTUDIANTE')")
    @Transactional(readOnly = true)
    public ProcesosAbiertosDTO procesosAbiertos(Integer id) {
        buscarOFallar(id);
        return contarProcesosAbiertos(id);
    }

    // Si el estudiante tiene procesos abiertos, la baja exige confirmar=true (409 si no): así el aviso no depende de
    // que el cliente lo muestre. Los procesos no se cancelan: la baja es lógica y el historial se conserva.
    @PreAuthorize("hasAuthority('DESACTIVAR_ESTUDIANTE')")
    @Transactional
    public void desactivar(Integer id, boolean confirmar) {
        Estudiante estudiante = buscarOFallar(id);
        if (!confirmar) {
            ProcesosAbiertosDTO procesos = contarProcesosAbiertos(id);
            if (procesos.hayProcesosAbiertos()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El estudiante tiene " + procesos.describir() + ". Confirmá la baja para continuar.");
            }
        }
        estudiante.setEstado(EstadoLogico.INACTIVO);
        estudianteRepository.save(estudiante);
    }

    private ProcesosAbiertosDTO contarProcesosAbiertos(Integer id) {
        LocalDateTime ahora = LocalDateTime.now();
        return new ProcesosAbiertosDTO(
                instanciaComunRepository.contarFuturasDelEstudiante(id, EstadoLogico.ACTIVO, ahora),
                recordatorioRepository.contarPendientesDelEstudiante(id, EstadoLogico.ACTIVO,
                        ahora.toLocalDate().atStartOfDay()));
    }

    @PreAuthorize("hasAuthority('REACTIVAR_ESTUDIANTE')")
    @Transactional
    public void reactivar(Integer id) {
        Estudiante estudiante = buscarOFallar(id);

        if (estudiante.getEstado() != EstadoLogico.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede reactivar a un estudiante inactivo.");
        }

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

    private void asignarTelefonos(Estudiante estudiante, List<String> numerosNuevos) {
        if (numerosNuevos == null) return;

        List<Telefono> actuales = estudiante.getTelefonos();
        if (actuales == null) {
            actuales = new ArrayList<>();
            estudiante.setTelefonos(actuales);
        }

        // Marcar como ELIMINADO los que ya no están en la lista
        for (Telefono tel : actuales) {
            if (!numerosNuevos.contains(tel.getId().getNumero())) {
                tel.setEstado(EstadoLogico.ELIMINADO);
            }
        }

        // Agregar los nuevos o reactivar los eliminados
        for (String numero : numerosNuevos) {
            boolean encontrado = false;
            for (Telefono tel : actuales) {
                if (tel.getId().getNumero().equals(numero)) {
                    tel.setEstado(EstadoLogico.ACTIVO);
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                actuales.add(new Telefono(estudiante, numero));
            }
        }
    }

    private void asignarGrupos(Estudiante estudiante, List<Integer> idGrupos) {
        if (idGrupos == null) return;
        List<Grupo> grupos = new ArrayList<>();
        for (Integer idGrupo : idGrupos) {
            Grupo grupo = grupoRepository.findById(idGrupo)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo no encontrado: " + idGrupo));
            
            // 422 (no 400): el JSON de la request es válido, el problema es que
            // idGrupo referencia un grupo inactivo — mismo código que el resto de
            // las validaciones de "entidad referenciada inactiva" del proyecto.
            if (grupo.getEstado() != EstadoLogico.ACTIVO) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "No se puede asignar un grupo que no está ACTIVO: " + idGrupo);
            }
            
            grupos.add(grupo);
        }
        estudiante.setGrupos(grupos);
    }

    // El bloque de salud/motivo de derivación requiere VER_BLOQUE_CONFIDENCIAL (asignado junto con
    // VER_ESTUDIANTE a PSICOPEDAGOGO y LEGALES). Se nulifica acá en vez de en el mapper para no
    // acoplar MapStruct a SecurityContextHolder.
    private EstudianteResponseDTO ocultarBloqueConfidencialSinPermiso(EstudianteResponseDTO dto) {
        if (!tienePermiso("VER_BLOQUE_CONFIDENCIAL")) {
            dto.setInformacionSalud(null);
            dto.setSistemaSalud(null);
            dto.setMotivoDerivacion(null);
        }
        return dto;
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