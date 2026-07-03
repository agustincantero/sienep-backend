package com.fides.sienep.service;

import com.fides.sienep.model.Funcionario;
import com.fides.sienep.model.FuncionarioGcal;
import com.fides.sienep.repository.FuncionarioGcalRepository;
import com.fides.sienep.repository.FuncionarioRepository;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.CalendarScopes;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);

    private static final List<String> SCOPES = List.of(CalendarScopes.CALENDAR_EVENTS);

    private final FuncionarioRepository    funcionarioRepository;
    private final FuncionarioGcalRepository funcionarioGcalRepository;
    private final OAuthStateStore          oAuthStateStore;

    @Value("${google.oauth2.client-id}")
    private String clientId;

    @Value("${google.oauth2.client-secret}")
    private String clientSecret;

    @Value("${google.oauth2.redirect-uri}")
    private String redirectUri;

    // ── Generar URL de autorización ───────────────────────────────────────
    // El frontend redirige al usuario a esta URL.
    // El parámetro state es un token opaco y aleatorio generado por OAuthStateStore,
    // NO el idUsuario en texto plano — eso era la vulnerabilidad original: cualquiera
    // podía adivinar el id de otro funcionario y completar el callback en su nombre.

    public String generarUrlAutorizacion(int idUsuario) {
        String state = oAuthStateStore.generarState(idUsuario);

        GoogleAuthorizationCodeRequestUrl url = new GoogleAuthorizationCodeRequestUrl(
                clientId,
                redirectUri,
                SCOPES
        );
        return url
                .setAccessType("offline")   // necesario para recibir refresh_token
                .set("prompt", "consent") // fuerza que Google siempre entregue refresh_token
                .setState(state)
                .build();
    }

    // ── Procesar callback de Google ───────────────────────────────────────
    // Recibe el code que Google nos manda, lo intercambia por tokens,
    // y guarda el refresh_token en BD asociado al funcionario.
    //
    // El idUsuario NUNCA sale del state que presenta el cliente: se resuelve
    // consumiendo el token contra OAuthStateStore, que es la única fuente de verdad
    // de "a qué funcionario corresponde este callback". Si el token no existe (nunca
    // se generó, ya se usó, o venció), se rechaza con 400 sin tocar ningún dato.

    @Transactional
    public void procesarCallback(String code, String state) {
        int idUsuario = oAuthStateStore.consumir(state)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El parámetro state del callback es inválido, ya fue usado o venció. " +
                                "Volvé a iniciar la conexión desde /api/auth/google/init."));

        Funcionario funcionario = funcionarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró un funcionario con el id " + idUsuario));

        GoogleTokenResponse tokenResponse;
        try {
            tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientId,
                    clientSecret,
                    code,
                    redirectUri
            ).execute();
        } catch (IOException e) {
            log.error("[GCAL] Error al intercambiar code por token para funcionario {}: {}",
                    idUsuario, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo completar la autorización con Google");
        }

        String refreshToken = tokenResponse.getRefreshToken();
        if (refreshToken == null) {
            // Puede pasar si el usuario ya autorizó antes y no se usó setApprovalPrompt("force")
            log.warn("[GCAL] Google no retornó refresh_token para funcionario {}. " +
                    "Verificar que setApprovalPrompt=force esté activo.", idUsuario);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Google no retornó el token de actualización. Intentá desconectar y reconectar la cuenta.");
        }

        // Email institucional del funcionario — coincide con su cuenta Google Workspace de UTEC,
        // por lo que no es necesario extraerlo del id_token de Google.
        String gcalEmail = funcionario.getEmail();

        Optional<FuncionarioGcal> gcalExistente = funcionarioGcalRepository
                .findByFuncionarioIdUsuario(idUsuario);

        if (gcalExistente.isPresent()) {
            // Ya tenía integración — actualizamos el token (puede ser que re-autorizó)
            FuncionarioGcal gcal = gcalExistente.get();
            gcal.setRefreshToken(refreshToken);
            gcal.setGcalEmail(gcalEmail);
            gcal.setAutorizado(true);
            funcionarioGcalRepository.save(gcal);
            log.info("[GCAL] Token actualizado para funcionario {}", idUsuario);
        } else {
            FuncionarioGcal gcal = new FuncionarioGcal(funcionario, refreshToken, gcalEmail);
            funcionarioGcalRepository.save(gcal);
            log.info("[GCAL] Integración de Google Calendar activada para funcionario {}", idUsuario);
        }
    }

    // ── Obtener access_token fresco desde refresh_token ───────────────────
    // El access_token de Google expira cada hora.
    // Este método obtiene uno nuevo usando el refresh_token guardado en BD.
    // Lo llama GoogleCalendarService antes de cada operación.

    public String obtenerAccessToken(String refreshToken) {
        try {
            TokenResponse response = new GoogleRefreshTokenRequest(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    refreshToken,
                    clientId,
                    clientSecret
            ).execute();

            return response.getAccessToken();
        } catch (IOException e) {
            log.error("[GCAL] Error al refrescar access_token: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo renovar el token de Google Calendar");
        }
    }
}