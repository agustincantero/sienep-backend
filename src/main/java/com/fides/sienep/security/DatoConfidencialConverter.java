package com.fides.sienep.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Se aplica con @Convert en cada campo confidencial (no autoApply: el resto de
// los String no se cifran). Hibernate obtiene la instancia desde Spring, por eso
// puede recibir el cifrador por constructor.
@Component
@Converter
@RequiredArgsConstructor
public class DatoConfidencialConverter implements AttributeConverter<String, String> {

    private final CifradorDatosConfidenciales cifrador;

    @Override
    public String convertToDatabaseColumn(String valor) {
        return cifrador.cifrar(valor);
    }

    @Override
    public String convertToEntityAttribute(String valor) {
        return cifrador.descifrar(valor);
    }
}
