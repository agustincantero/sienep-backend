package com.fides.sienep.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

// Facade sobre GoogleIdTokenVerifier: valida firma, emisor, vencimiento y
// audience (google.oauth.client-id) del ID token que el frontend obtiene vía
// Google Identity Services, y expone solo el email ya verificado por Google —
// AuthService no necesita conocer nada del SDK.
@Service
public class GoogleIdTokenVerifierService {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifierService.class);

    @Value("${google.oauth.client-id:}")
    private String clientId;

    // Construido una sola vez (lazy) y reutilizado, mismo criterio que el
    // Calendar.Builder de GoogleCalendarService.
    private GoogleIdTokenVerifier verifier;

    public Optional<String> emailVerificado(String idTokenString) {
        try {
            GoogleIdToken idToken = obtenerVerifier().verify(idTokenString);
            if (idToken == null) {
                log.warn("[GOOGLE-AUTH] ID token inválido: firma, emisor o audience no coinciden");
                return Optional.empty();
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                log.warn("[GOOGLE-AUTH] Login rechazado: Google no verificó el email ({})", payload.getEmail());
                return Optional.empty();
            }

            return Optional.of(payload.getEmail().toLowerCase());
        } catch (Exception e) {
            log.warn("[GOOGLE-AUTH] No se pudo verificar el ID token: {}: {}",
                    e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    private synchronized GoogleIdTokenVerifier obtenerVerifier() throws Exception {
        if (verifier != null) {
            return verifier;
        }

        verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();

        return verifier;
    }
}
