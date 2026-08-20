package com.fides.sienep.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class BrevoConfig {

    // Mismo criterio que los timeouts de spring.mail.properties.mail.smtp.*:
    // fallar rápido ante una dependencia externa caída en vez de colgar
    // indefinidamente el thread que la llama.
    @Bean
    public RestClient brevoRestClient(@Value("${brevo.api-key}") String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        return RestClient.builder()
                .baseUrl("https://api.brevo.com/v3/smtp/email")
                .requestFactory(requestFactory)
                .defaultHeader("api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
