package com.fides.sienep.config;

import com.fides.sienep.security.CifradorDatosConfidenciales;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

// Cifra al iniciar los datos confidenciales que quedaron en claro de antes de que
// existiera el cifrado en reposo (ver CifradorDatosConfidenciales). Es idempotente:
// solo toca los valores sin el prefijo "enc:v1:", así que después de la primera
// corrida no encuentra nada que hacer.
//
// Va por JDBC y no por las entidades para poder apagar, solo dentro de la
// transacción, el trigger que actualiza updated_at: si no, todos los comentarios
// confidenciales quedarían marcados como "editado" (editado = updatedAt > createdAt).
// El trigger de auditoría sigue activo; como ya omite estas columnas, registra un
// UPDATE por fila sin exponer el contenido.
//
// Se puede apagar con CIFRADO_MIGRAR_AL_INICIAR=false (los tests lo apagan).
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cifrado.migrar-al-iniciar", havingValue = "true")
public class MigracionCifradoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionCifradoRunner.class);

    private record TablaConfidencial(String tabla, String columnaId, List<String> columnas, String triggerUpdatedAt) {
    }

    private static final List<TablaConfidencial> TABLAS = List.of(
            new TablaConfidencial("estudiantes", "id_usuario",
                    List.of("inf_esta_salud", "sis_salud", "motivo"), "trg_actualizar_estudiantes"),
            new TablaConfidencial("estu_obs_confidenciales", "id_obs_confidencial",
                    List.of("obs_confidencial"), "trg_actualizar_estu_obs_confidenciales"),
            new TablaConfidencial("inst_com_confidenciales", "id_com_confidencial",
                    List.of("comentario"), "trg_actualizar_inst_com_confidenciales"));

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final CifradorDatosConfidenciales cifrador;

    @Override
    public void run(ApplicationArguments args) {
        for (TablaConfidencial t : TABLAS) {
            try {
                Integer cifradas = transaction.execute(status -> migrar(t));
                if (cifradas != null && cifradas > 0) {
                    log.info("[AUDIT] Cifrado en reposo: {} filas de {} cifradas", cifradas, t.tabla());
                }
            } catch (RuntimeException e) {
                // No se corta el arranque: los valores en claro se siguen leyendo bien y se
                // reintenta en el próximo inicio.
                log.error("[AUDIT] Cifrado en reposo: no se pudo migrar {}: {}", t.tabla(),
                        e instanceof DataAccessException dae ? causa(dae) : e.getMessage());
            }
        }
    }

    private int migrar(TablaConfidencial t) {
        String tabla = "proyecto." + t.tabla();
        String algunaEnClaro = t.columnas().stream()
                .map(c -> "(" + c + " IS NOT NULL AND " + c + " NOT LIKE 'enc:v1:%')")
                .reduce((a, b) -> a + " OR " + b).orElseThrow();
        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT " + t.columnaId() + ", " + String.join(", ", t.columnas())
                        + " FROM " + tabla + " WHERE " + algunaEnClaro + " FOR UPDATE");
        if (filas.isEmpty()) return 0;

        jdbc.execute("ALTER TABLE " + tabla + " DISABLE TRIGGER " + t.triggerUpdatedAt());
        String update = "UPDATE " + tabla + " SET "
                + String.join(", ", t.columnas().stream().map(c -> c + " = ?").toList())
                + " WHERE " + t.columnaId() + " = ?";
        int cifradas = 0;
        for (Map<String, Object> fila : filas) {
            Object id = fila.get(t.columnaId());
            Object[] valores = new Object[t.columnas().size() + 1];
            for (int i = 0; i < t.columnas().size(); i++) {
                String valor = (String) fila.get(t.columnas().get(i));
                valores[i] = cifrador.estaCifrado(valor) ? valor : cifrador.cifrar(valor);
            }
            valores[valores.length - 1] = id;
            // Savepoint por fila: en Postgres un error aborta toda la transacción, y una
            // fila que ya no cumple algún CHECK (ej. chk_edad_rango, que depende de la
            // fecha actual) no debe impedir cifrar las demás. Esa fila queda en claro.
            jdbc.execute("SAVEPOINT cifrar_fila");
            try {
                jdbc.update(update, valores);
                jdbc.execute("RELEASE SAVEPOINT cifrar_fila");
                cifradas++;
            } catch (DataAccessException e) {
                jdbc.execute("ROLLBACK TO SAVEPOINT cifrar_fila");
                log.error("[AUDIT] Cifrado en reposo: no se pudo cifrar {} {}={}: {}",
                        t.tabla(), t.columnaId(), id, causa(e));
            }
        }
        jdbc.execute("ALTER TABLE " + tabla + " ENABLE TRIGGER " + t.triggerUpdatedAt());
        return cifradas;
    }

    // Solo el mensaje de Postgres, sin el SQL ni el "Detail" (que incluye la fila con los valores cifrados).
    private static String causa(DataAccessException e) {
        String mensaje = e.getMostSpecificCause().getMessage();
        int detalle = mensaje == null ? -1 : mensaje.indexOf("\n");
        return detalle < 0 ? mensaje : mensaje.substring(0, detalle);
    }
}
