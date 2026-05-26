package com.fides.sienep.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Clave primaria compuesta de la tabla estu_telefonos.
 * PRIMARY KEY (id_usuario, telefono)
 *
 * @Embeddable: esta clase representa un "valor incrustado" dentro de la entidad
 * que la usa. No tiene tabla propia — sus columnas pertenecen a estu_telefonos.
 *
 * Diferencia con @IdClass:
 *   - @IdClass duplica los campos: aparecen en la entidad Y en la clase ID.
 *   - @Embeddable los define una sola vez aqui. La entidad tiene un unico campo
 *     @EmbeddedId que referencia este objeto.
 *   - El resultado es que la PK es un value object de primera clase, lo que
 *     modela mejor la realidad: (id_usuario + telefono) forman UNA clave, no dos campos sueltos.
 *
 * Reglas obligatorias para claves compuestas en JPA:
 *   - Debe implementar Serializable.
 *   - Debe tener equals() y hashCode() correctos (Lombok los genera).
 *   - Debe tener constructor sin argumentos.
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "Clave primaria compuesta del telefono: estudiante + numero")
public class TelefonoId implements Serializable {

    @Column(name = "id_usuario", nullable = false)
    @Schema(description = "ID del estudiante", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer idUsuario;

    @Column(name = "telefono", nullable = false, length = 15)
    @Schema(description = "Numero de telefono", example = "099123456")
    private String numero;
}