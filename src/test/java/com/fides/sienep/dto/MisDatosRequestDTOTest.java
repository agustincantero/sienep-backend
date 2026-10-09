package com.fides.sienep.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MisDatosRequestDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private MisDatosRequestDTO valido() {
        MisDatosRequestDTO dto = new MisDatosRequestDTO();
        dto.setCalle("Av 18 de Julio");
        dto.setNroPuerta(1234);
        dto.setCiudad("Montevideo");
        dto.setDepartamento("Montevideo");
        dto.setTelefonos(List.of("099123456"));
        return dto;
    }

    private Set<String> camposInvalidos(MisDatosRequestDTO dto) {
        Set<ConstraintViolation<MisDatosRequestDTO>> violaciones = validator.validate(dto);
        return violaciones.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Datos de contacto válidos - sin errores")
    void valido_SinErrores() {
        assertTrue(camposInvalidos(valido()).isEmpty());
    }

    @Test
    @DisplayName("Todos los campos vacíos - válido (es un reemplazo completo, se pueden borrar)")
    void todoVacio_SinErrores() {
        MisDatosRequestDTO dto = new MisDatosRequestDTO();
        dto.setTelefonos(List.of());
        assertTrue(camposInvalidos(dto).isEmpty());
    }

    @Test
    @DisplayName("Calle con punto - rechazada con el mismo patrón que el alta")
    void calleConPunto_Rechazada() {
        MisDatosRequestDTO dto = valido();
        dto.setCalle("Av. 18 de Julio");
        assertEquals(Set.of("calle"), camposInvalidos(dto));
    }

    @Test
    @DisplayName("Ciudad y departamento con símbolos - rechazados")
    void ciudadYDepartamentoConSimbolos_Rechazados() {
        MisDatosRequestDTO dto = valido();
        dto.setCiudad("Paso de los Toros!");
        dto.setDepartamento("Río Negro.");
        assertEquals(Set.of("ciudad", "departamento"), camposInvalidos(dto));
    }

    @Test
    @DisplayName("Teléfono con guiones - rechazado")
    void telefonoConGuiones_Rechazado() {
        MisDatosRequestDTO dto = valido();
        dto.setTelefonos(List.of("099-123-45"));
        assertEquals(1, validator.validate(dto).size());
    }

    @Test
    @DisplayName("Teléfono de 13 dígitos - rechazado (máximo 12)")
    void telefonoDe13Digitos_Rechazado() {
        MisDatosRequestDTO dto = valido();
        dto.setTelefonos(List.of("5980991234567"));
        assertEquals(1, validator.validate(dto).size());
    }

    @Test
    @DisplayName("Teléfonos de 8 y 12 dígitos - aceptados (límites)")
    void telefonosEnLimites_Aceptados() {
        MisDatosRequestDTO dto = valido();
        dto.setTelefonos(List.of("22001122", "598099123456"));
        assertTrue(camposInvalidos(dto).isEmpty());
    }
}
