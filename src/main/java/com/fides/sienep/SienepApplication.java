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
@EnableCaching
@EnableScheduling
public class SienepApplication {

    public static void main(String[] args) {
        SpringApplication.run(SienepApplication.class, args);
    }

}