package com.fides.sienep.service;

import com.fides.sienep.repository.RecordatorioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstadoOperativoSchedulerTest {

    @Mock private RecordatorioRepository recordatorioRepository;

    @InjectMocks
    private EstadoOperativoScheduler scheduler;

    @Test
    @DisplayName("La corrida programada delega el recálculo en la base")
    void recalcular_delegaEnRepositorio() {
        when(recordatorioRepository.recalcularEstadosOperativos()).thenReturn(3);

        scheduler.recalcularEstadosOperativos();

        verify(recordatorioRepository).recalcularEstadosOperativos();
    }

    @Test
    @DisplayName("Al arrancar, un fallo del recálculo no impide que la aplicación levante")
    void recalcularAlArrancar_fallo_noRelanza() {
        when(recordatorioRepository.recalcularEstadosOperativos())
                .thenThrow(new InvalidDataAccessResourceUsageException("function does not exist"));

        assertDoesNotThrow(() -> scheduler.recalcularAlArrancar());
        verify(recordatorioRepository).recalcularEstadosOperativos();
    }
}
