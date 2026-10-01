package com.fides.sienep.mapper;

import com.fides.sienep.dto.SolicitudInstanciaResponseDTO;
import com.fides.sienep.model.EstadoSolicitud;
import com.fides.sienep.model.Estudiante;
import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.InstanciaComun;
import com.fides.sienep.model.SolicitudInstancia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SolicitudInstanciaMapperTest {

    private final SolicitudInstanciaMapper mapper = new SolicitudInstanciaMapperImpl();

    private SolicitudInstancia solicitud() {
        Estudiante estudiante = new Estudiante();
        estudiante.setIdUsuario(5);
        estudiante.setNombre("Ana");
        estudiante.setApellido("García");
        SolicitudInstancia solicitud = new SolicitudInstancia(estudiante, "Quiero una reunión");
        solicitud.setIdSolicitud(1);
        return solicitud;
    }

    @Test
    @DisplayName("Pendiente - Sin funcionario ni instancia, esos campos quedan en null")
    void toDto_Pendiente_SinResolucion() {
        SolicitudInstanciaResponseDTO dto = mapper.toDto(solicitud());

        assertEquals(5, dto.getIdEstudiante());
        assertEquals("Ana García", dto.getNombreEstudiante());
        assertEquals(EstadoSolicitud.PENDIENTE, dto.getEstado());
        assertNull(dto.getIdFuncionario());
        assertNull(dto.getNombreFuncionario());
        assertNull(dto.getCodInstancia());
        assertNull(dto.getIdNegInstancia());
    }

    @Test
    @DisplayName("Atendida - Incluye quién la atendió y la instancia generada")
    void toDto_Atendida_ConResolucion() {
        SolicitudInstancia solicitud = solicitud();
        Funcionario funcionario = new Funcionario();
        funcionario.setIdUsuario(10);
        funcionario.setNombre("Carlos");
        funcionario.setApellido("Pérez");
        InstanciaComun instancia = new InstanciaComun();
        instancia.setCodInstancia(77);
        instancia.setIdNegInstancia("INS-2026-0077");
        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        solicitud.setFuncionario(funcionario);
        solicitud.setInstancia(instancia);

        SolicitudInstanciaResponseDTO dto = mapper.toDto(solicitud);

        assertEquals(10, dto.getIdFuncionario());
        assertEquals("Carlos Pérez", dto.getNombreFuncionario());
        assertEquals(77, dto.getCodInstancia());
        assertEquals("INS-2026-0077", dto.getIdNegInstancia());
    }
}
