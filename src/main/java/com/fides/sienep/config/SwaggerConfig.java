package com.fides.sienep.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        // Sin .servers(...): springdoc detecta el server automáticamente a
        // partir del request real a /v3/api-docs (respeta X-Forwarded-*
        // gracias a server.forward-headers-strategy=framework), así que
        // funciona en cualquier puerto local y no hay URL que mantener.
        return new OpenAPI()
                .info(new Info()
                        .title("SIENEP API")
                        .version("1.0.0")
                        .description("Sistema Integral de Estudiantes con Necesidades Educativas Personalizadas")
                        .contact(new Contact().name("Fides")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}