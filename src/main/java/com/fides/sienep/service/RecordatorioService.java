package com.fides.sienep.service;

import com.fides.sienep.dto.RecordatorioRequestDTO;
import com.fides.sienep.dto.RecordatorioResponseDTO;
import com.fides.sienep.model.*;
import com.fides.sienep.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordatorioService {

    private final RecordatorioRepository recordatorioRepository;
    private final InstanciaRepository instanciaRepository;
    private final CategoriaRecordatorioRepository categoriaRecordatorioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final FrecuenciaRepository frecuenciaRepository;

    public RecordatorioResponseDTO crear(RecordatorioRequestDTO dto) {
        Instancia instancia = instanciaRepository.findById(dto.getCodInstancia())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una instancia con el id " + dto.getCodInstancia()));

        CategoriaRecordatorio categoria = categoriaRecordatorioRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una categoría con el id " + dto.getIdCategoria()));

        Funcionario responsable = funcionarioRepository.findById(dto.getIdResponsable())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró un funcionario con el id " + dto.getIdResponsable()));

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
                    .orElseThrow(() -> new EntityNotFoundException("No se encontró una frecuencia con el id " + dto.getIdFrecuencia()));
            recordatorio.setFrecuencia(frecuencia);
        }

        recordatorio.setFechaFin(dto.getFechaFin());
        recordatorio.setNroOcurrencias(dto.getNroOcurrencias());

        return toResponseDTO(recordatorioRepository.save(recordatorio));
    }

    public List<RecordatorioResponseDTO> listarActivos() {
        List<Recordatorio> recordatorios = recordatorioRepository.findByEstado(EstadoLogico.ACTIVO);
        List<RecordatorioResponseDTO> resultado = new ArrayList<>();
        for (Recordatorio recordatorio : recordatorios) {
            resultado.add(toResponseDTO(recordatorio));
        }
        return resultado;
    }

    public RecordatorioResponseDTO buscarPorId(int id) {
        return toResponseDTO(buscarActivoOFallar(id));
    }

    public List<RecordatorioResponseDTO> listarPorInstancia(int codInstancia) {
        List<Recordatorio> recordatorios = recordatorioRepository.findByInstanciaAndEstado(codInstancia, EstadoLogico.ACTIVO);
        List<RecordatorioResponseDTO> resultado = new ArrayList<>();
        for (Recordatorio recordatorio : recordatorios) {
            resultado.add(toResponseDTO(recordatorio));
        }
        return resultado;
    }

    public List<RecordatorioResponseDTO> listarPorResponsable(int idResponsable) {
        List<Recordatorio> recordatorios = recordatorioRepository.findByResponsableAndEstado(idResponsable, EstadoLogico.ACTIVO);
        List<RecordatorioResponseDTO> resultado = new ArrayList<>();
        for (Recordatorio recordatorio : recordatorios) {
            resultado.add(toResponseDTO(recordatorio));
        }
        return resultado;
    }

    public RecordatorioResponseDTO actualizar(int id, RecordatorioRequestDTO dto) {
        Recordatorio recordatorio = buscarActivoOFallar(id);

        Instancia instancia = instanciaRepository.findById(dto.getCodInstancia())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una instancia con el id " + dto.getCodInstancia()));

        CategoriaRecordatorio categoria = categoriaRecordatorioRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró una categoría con el id " + dto.getIdCategoria()));

        Funcionario responsable = funcionarioRepository.findById(dto.getIdResponsable())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró un funcionario con el id " + dto.getIdResponsable()));

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
                    .orElseThrow(() -> new EntityNotFoundException("No se encontró una frecuencia con el id " + dto.getIdFrecuencia()));
            recordatorio.setFrecuencia(frecuencia);
        } else {
            recordatorio.setFrecuencia(null);
        }

        return toResponseDTO(recordatorioRepository.save(recordatorio));
    }

    public void darDeBaja(int id) {
        Recordatorio recordatorio = buscarActivoOFallar(id);
        recordatorio.setEstado(EstadoLogico.INACTIVO);
        recordatorioRepository.save(recordatorio);
    }

    private Recordatorio buscarActivoOFallar(int id) {
        return recordatorioRepository.findByCodRecordatorioAndEstado(id, EstadoLogico.ACTIVO)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró un recordatorio activo con el id " + id));
    }

    private RecordatorioResponseDTO toResponseDTO(Recordatorio r) {
        RecordatorioResponseDTO dto = new RecordatorioResponseDTO();
        dto.setCodRecordatorio(r.getCodRecordatorio());
        dto.setIdNegRecordatorio(r.getIdNegRecordatorio());
        dto.setTitulo(r.getTitulo());
        dto.setTipo(r.getTipo());
        dto.setFechaHora(r.getFechaHora());
        dto.setCodInstancia(r.getInstancia().getCodInstancia());
        dto.setTituloInstancia(r.getInstancia().getTitulo());
        dto.setIdCategoria(r.getCategoria().getIdCategoria());
        dto.setNombreCategoria(r.getCategoria().getNomCateRecordatorios());
        dto.setIdResponsable(r.getResponsable().getIdUsuario());
        dto.setNombreResponsable(r.getResponsable().getNombre() + " " + r.getResponsable().getApellido());
        if (r.getFrecuencia() != null) {
            dto.setIdFrecuencia(r.getFrecuencia().getIdFrecuencia());
            dto.setDescripcionFrecuencia(r.getFrecuencia().getDescripcion());
        }
        dto.setFechaFin(r.getFechaFin());
        dto.setNroOcurrencias(r.getNroOcurrencias());
        dto.setEstadoOperativo(r.getEstadoOperativo());
        dto.setEstado(r.getEstado());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }
}