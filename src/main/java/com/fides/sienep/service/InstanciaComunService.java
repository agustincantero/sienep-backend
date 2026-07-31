package com.fides.sienep.service;

import com.fides.sienep.dto.InstanciaComunRequestDTO;
import com.fides.sienep.dto.InstanciaComunResponseDTO;
import com.fides.sienep.dto.InstanciaComunUpdateDTO;
import com.fides.sienep.dto.InstanciaDesdeRecordatorioDTO;
import com.fides.sienep.dto.RecordatorioInicialDTO;
import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.mapper.InstanciaComunMapper;
import com.fides.sienep.model.CategoriaInstancia;
import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.Instancia;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.model.Recordatorio;
import com.fides.sienep.repository.CategoriaInstanciaRepository;
import com.fides.sienep.repository.EstudianteRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.fides.sienep.repository.InstanciaComunRepository;
import com.fides.sienep.repository.RecordatorioRepository;
import com.fides.sienep.utils.InstanciaIdGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InstanciaComunService {

    private static final Logger log = LoggerFactory.getLogger(InstanciaComunService.class);

    private final InstanciaComunRepository instanciaComunRepository;
    private final EstudianteRepository estudianteRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final CategoriaInstanciaRepository categoriaInstanciaRepository;
    private final InstanciaIdGenerator instanciaIdGenerator;
    private final InstanciaComunMapper instanciaComunMapper;
    private final EmailService emailService;
    private final RecordatorioService recordatorioService;
    private final RecordatorioRepository recordatorioRepository;

    private Integer getIdUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Integer.parseInt(auth.getName());
    }

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_INSTANCIA')")
    public InstanciaComunResponseDTO crear(InstanciaComunRequestDTO dto) {
        Estudiante estudiante = estudianteRepository.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un estudiante con el id " + dto.getIdEstudiante()));

        if (estudiante.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "El estudiante con id " + dto.getIdEstudiante() + " no está activo");
        }

        Funcionario funcionario = funcionarioRepository.findById(getIdUsuarioAutenticado())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el funcionario autenticado"));

        if (funcionario.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "El funcionario autenticado no está activo");
        }

        CategoriaInstancia categoria = categoriaInstanciaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        if (categoria.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "La categoría con id " + dto.getIdCategoria() + " no está activa");
        }

        InstanciaComun instancia = new InstanciaComun(
                dto.getTitulo(),
                dto.getFechaHora(),
                estudiante,
                funcionario,
                dto.getCanal(),
                categoria
        );

        instancia.setIdNegInstancia(instanciaIdGenerator.generarIdNeg());

        InstanciaComunResponseDTO response = instanciaComunMapper.toDto(
                instanciaComunRepository.save(instancia));

        // RF15 — notificar al funcionario creador con el ID de negocio generado.
        // El try-catch externo es una segunda capa de defensa: aunque enviarNotificacionInstancia
        // ya no relanza, esto garantiza que cualquier excepción inesperada no revierta
        // la transacción de @Transactional.
        try {
            emailService.enviarNotificacionInstancia(
                    funcionario.getEmail(),
                    funcionario.getNombre(),
                    response.getIdNegInstancia(),
                    response.getTitulo()
            );
        } catch (Exception e) {
            log.warn("[AUDIT] Fallo al notificar instancia {} al funcionario {}: {}",
                    response.getIdNegInstancia(), funcionario.getIdUsuario(), e.getMessage());
        }

        // RF13 — recordatorio inicial opcional, con sync a Google Calendar heredado de RF22.
        // A diferencia del email y de Google Calendar (servicios externos, "best effort" con
        // try/catch propio adentro de RecordatorioService), acá NO atrapamos la excepción:
        // si el funcionario pidió explícitamente un recordatorio y los datos son inválidos
        // (categoría inexistente, frecuencia inactiva, etc.), es mejor que falle todo el POST
        // con un 404/422 claro que crear una instancia "a medias" sin el recordatorio pedido.
        // Al estar dentro del mismo @Transactional, si recordatorioService.crear() lanza
        // ResponseStatusException, se hace rollback también de la instancia recién guardada.
        if (dto.getRecordatorioInicial() != null) {
            RecordatorioRequestDTO recordatorioDto = construirRecordatorioDesdeInstancia(
                    dto.getRecordatorioInicial(), response.getCodInstancia(), funcionario.getIdUsuario());

            RecordatorioResponseDTO recordatorioCreado = recordatorioService.crear(recordatorioDto);
            response.setRecordatorioCreado(recordatorioCreado);
        }

        return response;
    }

    // RF13 — arma el RecordatorioRequestDTO "completo" a partir del RecordatorioInicialDTO
    // (que no trae codInstancia ni, a veces, idResponsable).
    private RecordatorioRequestDTO construirRecordatorioDesdeInstancia(
            RecordatorioInicialDTO origen, int codInstancia, int idFuncionarioCreador) {

        RecordatorioRequestDTO dto = new RecordatorioRequestDTO();
        dto.setTitulo(origen.getTitulo());
        dto.setTipo(origen.getTipo());
        dto.setFechaHora(origen.getFechaHora());
        dto.setCodInstancia(codInstancia);
        dto.setIdCategoria(origen.getIdCategoria());
        dto.setIdResponsable(
                origen.getIdResponsable() != null ? origen.getIdResponsable() : idFuncionarioCreador);
        dto.setIdFrecuencia(origen.getIdFrecuencia());
        dto.setFechaFin(origen.getFechaFin());

        // Defensa en profundidad: este DTO se construye a mano y nunca pasa por @Valid,
        // así que el @Min(1) del RecordatorioInicialDTO no protege este camino.
        // nroOcurrencias=0 junto con una frecuencia produciría una recurrencia sin límite
        // en Google Calendar (regresión vía RF13).
        Integer nroOcurrencias = origen.getNroOcurrencias();
        if (nroOcurrencias != null && nroOcurrencias < 1) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "El número de ocurrencias del recordatorio inicial debe ser al menos 1");
        }
        dto.setNroOcurrencias(nroOcurrencias);
        return dto;
    }

    // RF27 — genera una instancia de seguimiento a partir de un recordatorio cumplido.
    // Vive acá y no en RecordatorioService a propósito: InstanciaComunService ya depende
    // de RecordatorioService (desde RF13), así que agregar la dependencia inversa crearía
    // un ciclo. RecordatorioService.marcarCumplido() no sabe nada de instancias.
    @Transactional
    @PreAuthorize("hasAuthority('CREAR_INSTANCIA')")
    public InstanciaComunResponseDTO crearDesdeRecordatorio(int codRecordatorio, InstanciaDesdeRecordatorioDTO dto) {
        // Lock pesimista: evita que dos requests concurrentes para el mismo
        // recordatorio pasen ambas el chequeo de "no cumplido" de abajo antes de
        // que cualquiera lo marque, lo que generaría instancias de seguimiento
        // duplicadas (ver comentario en RecordatorioRepository).
        Recordatorio recordatorio = recordatorioRepository
                .findByCodRecordatorioAndEstadoParaActualizar(codRecordatorio, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un recordatorio activo con el id " + codRecordatorio));

        if (recordatorio.isCumplido()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El recordatorio con id " + codRecordatorio + " ya generó una instancia");
        }

        Instancia instanciaOrigen = recordatorio.getInstancia();
        Estudiante estudiante = instanciaOrigen.getEstudiante();

        InstanciaComunRequestDTO instanciaDto = new InstanciaComunRequestDTO();
        instanciaDto.setTitulo(
                dto.getTitulo() != null && !dto.getTitulo().isBlank()
                        ? dto.getTitulo()
                        : "Seguimiento: " + recordatorio.getTitulo());
        instanciaDto.setFechaHora(LocalDateTime.now());
        instanciaDto.setIdEstudiante(estudiante.getIdUsuario());
        instanciaDto.setCanal(dto.getCanal());
        instanciaDto.setIdCategoria(dto.getIdCategoria());

        // Self-invocation intencional: this.crear() no pasa por el proxy de Spring, así que
        // su propio @Transactional/@PreAuthorize no se re-evalúan acá — corre como método
        // plano, dentro de la misma transacción que ya abrió crearDesdeRecordatorio(), y la
        // autorización ya se validó arriba. Es el comportamiento buscado, no un bug: si
        // alguna vez esto se refactoriza a una clase separada, hay que revisar este detalle.
        InstanciaComunResponseDTO nuevaInstancia = this.crear(instanciaDto);

        // Se marca cumplido recién después de que la instancia se creó con éxito: si crear()
        // lanza una excepción, el rollback de @Transactional deshace todo y el recordatorio
        // queda sin marcar, disponible para reintentar.
        recordatorioService.marcarCumplido(codRecordatorio);

        return nuevaInstancia;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public Page<InstanciaComunResponseDTO> listarActivas(Pageable pageable) {
        Page<InstanciaComun> pagina = instanciaComunRepository.findByEstado(EstadoLogico.ACTIVO, pageable);
        List<InstanciaComunResponseDTO> dtos = instanciaComunMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public InstanciaComunResponseDTO buscarPorId(int codInstancia) {
        return instanciaComunMapper.toDto(buscarActivaOFallar(codInstancia));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public Page<InstanciaComunResponseDTO> listarPorEstudiante(int idEstudiante, Pageable pageable) {
        Page<InstanciaComun> pagina = instanciaComunRepository.findByEstudianteAndEstado(
                idEstudiante, EstadoLogico.ACTIVO, pageable);
        List<InstanciaComunResponseDTO> dtos = instanciaComunMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public Page<InstanciaComunResponseDTO> listarPorFuncionario(int idFuncionario, Pageable pageable) {
        Page<InstanciaComun> pagina = instanciaComunRepository.findByFuncionarioAndEstado(
                idFuncionario, EstadoLogico.ACTIVO, pageable);
        List<InstanciaComunResponseDTO> dtos = instanciaComunMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_INSTANCIAS')")
    public Page<InstanciaComunResponseDTO> listarPorCategoria(int idCategoria, Pageable pageable) {
        Page<InstanciaComun> pagina = instanciaComunRepository.findByCategoriaAndEstado(
                idCategoria, EstadoLogico.ACTIVO, pageable);
        List<InstanciaComunResponseDTO> dtos = instanciaComunMapper.toDtoList(pagina.getContent());
        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }

    @PreAuthorize("hasRole('ESTUDIANTE')")
    @Transactional(readOnly = true)
    public List<InstanciaComunResponseDTO> listarMisInstancias() {
        Integer idEstudiante = getIdUsuarioAutenticado();
        return instanciaComunMapper.toDtoList(
                instanciaComunRepository.findByEstudianteAndEstado(idEstudiante, EstadoLogico.ACTIVO));
    }

    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_INSTANCIA')")
    public InstanciaComunResponseDTO actualizar(int codInstancia, InstanciaComunUpdateDTO dto) {
        InstanciaComun instancia = buscarActivaOFallar(codInstancia);

        CategoriaInstancia categoria = categoriaInstanciaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        if (categoria.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "La categoría con id " + dto.getIdCategoria() + " no está activa");
        }

        instancia.setTitulo(dto.getTitulo());
        instancia.setFechaHora(dto.getFechaHora());
        instancia.setCanal(dto.getCanal());
        instancia.setCategoria(categoria);

        return instanciaComunMapper.toDto(instanciaComunRepository.save(instancia));
    }

    @Transactional
    @PreAuthorize("hasAuthority('BAJA_INSTANCIA')")
    public void darDeBaja(int codInstancia) {
        InstanciaComun instancia = buscarActivaOFallar(codInstancia);
        instancia.setEstado(EstadoLogico.INACTIVO);
        instanciaComunRepository.save(instancia);
    }

    @Transactional
    @PreAuthorize("hasAuthority('REACTIVAR_INSTANCIA')")
    public InstanciaComunResponseDTO reactivar(int codInstancia) {
        InstanciaComun instancia = instanciaComunRepository.findById(codInstancia)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró una instancia con el id " + codInstancia));

        if (instancia.getEstado() == EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La instancia ya se encuentra activa");
        }

        instancia.setEstado(EstadoLogico.ACTIVO);
        InstanciaComun guardada = instanciaComunRepository.save(instancia);
        return instanciaComunMapper.toDto(guardada);
    }

    // ----------------------------------------------------------------
    // Helpers privados
    // ----------------------------------------------------------------

    private InstanciaComun buscarActivaOFallar(int codInstancia) {
        return instanciaComunRepository.findByCodInstanciaAndEstado(codInstancia, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una instancia activa con el id " + codInstancia));
    }
}