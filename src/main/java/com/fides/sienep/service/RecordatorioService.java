package com.fides.sienep.service;

import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.mapper.RecordatorioMapper;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.*;
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

    @Transactional
    @PreAuthorize("hasAuthority('CREAR_RECORDATORIO')")
    public RecordatorioResponseDTO crear(RecordatorioRequestDTO dto) {
        Instancia instancia = instanciaRepository.findById(dto.getCodInstancia())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una instancia con el id " + dto.getCodInstancia()));

        CategoriaRecordatorio categoria = categoriaRecordatorioRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        Funcionario responsable = funcionarioRepository.findById(dto.getIdResponsable())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un funcionario con el id " + dto.getIdResponsable()));

        Recordatorio recordatorio = new Recordatorio(
                dto.getTitulo(),
                dto.getTipo(),
                dto.getFechaHora(),
                instancia,
                categoria,
                responsable
        );

        if (dto.getIdFrecuencia() != null) {
            Frecuencia frecuencia = frecuenciaRepository.findById(dto.getIdFrecuencia())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No se encontró una frecuencia con el id " + dto.getIdFrecuencia()));
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

        CategoriaRecordatorio categoria = categoriaRecordatorioRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una categoría con el id " + dto.getIdCategoria()));

        Funcionario responsable = funcionarioRepository.findById(dto.getIdResponsable())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un funcionario con el id " + dto.getIdResponsable()));

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