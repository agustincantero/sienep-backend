package com.fides.sienep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// Se configura el orden en 0 para que @Transactional se ejecute antes (por fuera)
// de nuestros aspectos personalizados, asegurando que compartan la misma conexión.
@EnableTransactionManagement(order = 0)
// B2 — sin un CacheManager explícito en el classpath (Redis, etc.), Spring Boot
// auto-configura ConcurrentMapCacheManager: un caché en memoria del proceso, igual
// que OAuthStateStore/PasswordResetRateLimiter. Usado por los @Cacheable de
// RolService, CategoriaInstanciaService, CategoriaRecordatorioService,
// PermisoService, ItrService y CarreraService (catálogos que cambian poco).
// Limitación conocida y aceptada para el alcance actual (instancia única): si el
// backend llegara a escalar horizontalmente, cada instancia tendría su propio
// caché, así que un @CacheEvict en la instancia A no invalidaría la caché de la
// instancia B — quedaría sirviendo datos viejos hasta su propio próximo evict.
// Si eso pasa, migrar a un CacheManager compartido (Redis) antes de escalar.
@EnableCaching
@EnableScheduling
public class SienepApplication {

    public static void main(String[] args) {
        SpringApplication.run(SienepApplication.class, args);
    }

}