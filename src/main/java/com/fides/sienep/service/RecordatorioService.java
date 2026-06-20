package com.fides.sienep.service;

import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.mapper.RecordatorioMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.*;
import com.fides.sienep.utils.RecordatorioIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordatorioService {

    private final RecordatorioRepository recordatorioRepository;
    private final InstanciaRepository instanciaRepository;
    private final CategoriaRecordatorioRepository categoriaRecordatorioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FrecuenciaRepository frecuenciaRepository;
    private final RecordatorioMapper recordatorioMapper;
    private final RecordatorioIdGenerator recordatorioIdGenerator;

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_RECORDATORIO')")
    public RecordatorioResponseDTO crear(RecordatorioRequestDTO dto) {
        Instancia instancia = instanciaRepository.findById(dto.getCodInstancia())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una instancia con el id " + dto.getCodInstancia()));

        if (instancia.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "La instancia con id " + dto.getCodInstancia() + " no está activa");
        }

        CategoriaRecordatorio categoria = categoriaRecordatorioRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        if (categoria.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "La categoría con id " + dto.getIdCategoria() + " no está activa");
        }

        Funcionario responsable = funcionarioRepository.findById(dto.getIdResponsable())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un funcionario con el id " + dto.getIdResponsable()));

        if (responsable.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "El funcionario con id " + dto.getIdResponsable() + " no está activo");
        }

        Recordatorio recordatorio = new Recordatorio(
                dto.getTitulo(),
                dto.getTipo(),
                dto.getFechaHora(),
                instancia,
                categoria,
                responsable
        );

        recordatorio.setIdNegRecordatorio(recordatorioIdGenerator.generarIdNeg());

        if (dto.getIdFrecuencia() != null) {
            Frecuencia frecuencia = frecuenciaRepository.findById(dto.getIdFrecuencia())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No se encontró una frecuencia con el id " + dto.getIdFrecuencia()));

            if (frecuencia.getEstado() != EstadoLogico.ACTIVO) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "La frecuencia con id " + dto.getIdFrecuencia() + " no está activa");
            }
            recordatorio.setFrecuencia(frecuencia);
        }

        recordatorio.setFechaFin(dto.getFechaFin());
        recordatorio.setNroOcurrencias(dto.getNroOcurrencias());

        return recordatorioMapper.toDto(recordatorioRepository.save(recordatorio));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public List<RecordatorioResponseDTO> listarActivos() {
        return recordatorioMapper.toDtoList(
                recordatorioRepository.findByEstado(EstadoLogico.ACTIVO));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public RecordatorioResponseDTO buscarPorId(int id) {
        return recordatorioMapper.toDto(buscarActivoOFallar(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public List<RecordatorioResponseDTO> listarPorInstancia(int codInstancia) {
        return recordatorioMapper.toDtoList(
                recordatorioRepository.findByInstanciaAndEstado(codInstancia, EstadoLogico.ACTIVO));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('VER_RECORDATORIOS')")
    public List<RecordatorioResponseDTO> listarPorResponsable(int idResponsable) {
        return recordatorioMapper.toDtoList(
                recordatorioRepository.findByResponsableAndEstado(idResponsable, EstadoLogico.ACTIVO));
    }

    @Transactional
    @PreAuthorize("hasAuthority('EDITAR_RECORDATORIO')")
    public RecordatorioResponseDTO actualizar(int id, RecordatorioRequestDTO dto) {
        Recordatorio recordatorio = buscarActivoOFallar(id);

        Instancia instancia = instanciaRepository.findById(dto.getCodInstancia())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una instancia con el id " + dto.getCodInstancia()));

        if (instancia.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "La instancia con id " + dto.getCodInstancia() + " no está activa");
        }

        CategoriaRecordatorio categoria = categoriaRecordatorioRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        if (categoria.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "La categoría con id " + dto.getIdCategoria() + " no está activa");
        }

        Funcionario responsable = funcionarioRepository.findById(dto.getIdResponsable())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un funcionario con el id " + dto.getIdResponsable()));

        if (responsable.getEstado() != EstadoLogico.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "El funcionario con id " + dto.getIdResponsable() + " no está activo");
        }

        recordatorio.setTitulo(dto.getTitulo());
        recordatorio.setTipo(dto.getTipo());
        recordatorio.setFechaHora(dto.getFechaHora());
        recordatorio.setInstancia(instancia);
        recordatorio.setCategoria(categoria);
        recordatorio.setResponsable(responsable);
        recordatorio.setFechaFin(dto.getFechaFin());
        recordatorio.setNroOcurrencias(dto.getNroOcurrencias());

        if (dto.getIdFrecuencia() != null) {
            Frecuencia frecuencia = frecuenciaRepository.findById(dto.getIdFrecuencia())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No se encontró una frecuencia con el id " + dto.getIdFrecuencia()));

            if (frecuencia.getEstado() != EstadoLogico.ACTIVO) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "La frecuencia con id " + dto.getIdFrecuencia() + " no está activa");
            }
            recordatorio.setFrecuencia(frecuencia);
        } else {
            recordatorio.setFrecuencia(null);
        }

        return recordatorioMapper.toDto(recordatorioRepository.save(recordatorio));
    }

    @Transactional
    @PreAuthorize("hasAuthority('BAJA_RECORDATORIO')")
    public void darDeBaja(int id) {
        Recordatorio recordatorio = buscarActivoOFallar(id);
        recordatorio.setEstado(EstadoLogico.INACTIVO);
        recordatorioRepository.save(recordatorio);
    }

    private Recordatorio buscarActivoOFallar(int id) {
        return recordatorioRepository.findByCodRecordatorioAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un recordatorio activo con el id " + id));
    }
}