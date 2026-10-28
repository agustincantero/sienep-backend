package com.fides.sienep.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

// Conexión de solo lectura al Data Warehouse (rol dw_lectura). Mismo criterio que ActiveDirectoryVerifierService, el pool se arma recién en el primer uso y a partir de propiedades opcionales: así no hay un segundo DataSource bean que haga retroceder la autoconfiguración de JPA, y el contexto arranca igual sin DW configurado (perfil test, desarrollo). Si falta la configuración o la base no responde, se informa con 503 en vez de un error genérico.
@Component
public class DwJdbc {

    private static final Logger log = LoggerFactory.getLogger(DwJdbc.class);

    // No es constante para poder acortarlo en los tests con una base inalcanzable
    private long timeoutConexionMs = 5000;

    @Value("${dw.url:}")
    private String url;

    @Value("${dw.user:}")
    private String usuario;

    @Value("${dw.password:}")
    private String contrasenia;

    private HikariDataSource pool;
    private NamedParameterJdbcTemplate template;

    // Un fallo no se cachea: el pedido siguiente vuelve a intentar, por si el DW volvió o se corrigió la configuración.
    public synchronized NamedParameterJdbcTemplate template() {
        if (template != null) {
            return template;
        }
        if (!StringUtils.hasText(url) || !StringUtils.hasText(usuario)) {
            throw noDisponible("El Data Warehouse no está configurado", null);
        }

        HikariConfig config = new HikariConfig();
        config.setPoolName("dw-lectura");
        config.setJdbcUrl(url);
        config.setUsername(usuario);
        config.setPassword(contrasenia);
        config.setReadOnly(true);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(timeoutConexionMs);
        config.setInitializationFailTimeout(timeoutConexionMs);

        try {
            pool = new HikariDataSource(config);
        } catch (RuntimeException e) {
            log.warn("[WARN] No se pudo conectar al Data Warehouse: {}", e.getMessage());
            throw noDisponible("No se pudo conectar al Data Warehouse", e);
        }
        template = new NamedParameterJdbcTemplate(pool);
        return template;
    }

    @PreDestroy
    public synchronized void cerrar() {
        if (pool != null) {
            pool.close();
        }
    }

    public static ResponseStatusException noDisponible(String mensaje, Throwable causa) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, mensaje, causa);
    }
}
