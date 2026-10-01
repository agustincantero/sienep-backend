package com.fides.sienep.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

// Pedido de una instancia hecho por el propio estudiante desde "Mi perfil". Un funcionario la atiende
// (crea la instancia y queda enlazada en `instancia`) o la rechaza con una `respuesta` para el estudiante.
@Entity
@Table(name = "solicitudes_instancia", schema = "proyecto")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudInstancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Integer idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Column(name = "motivo", length = 500, nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", nullable = false, columnDefinition = "proyecto.estado_solicitud")
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "respuesta", length = 500)
    private String respuesta;

    // Quien la atendió o la rechazó; null mientras está PENDIENTE.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_funcionario")
    private Funcionario funcionario;

    // Instancia generada al atenderla; null salvo en ATENDIDA.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cod_instancia")
    private InstanciaComun instancia;

    @Column(name = "fec_resolucion")
    private LocalDateTime fechaResolucion;

    // Ver Carrera.java para el porqué de @CreationTimestamp/@UpdateTimestamp acá.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SolicitudInstancia(Estudiante estudiante, String motivo) {
        this.estudiante = estudiante;
        this.motivo = motivo;
    }
}
