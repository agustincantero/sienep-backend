package com.fides.sienep.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspecto encargado de interceptar las llamadas a los servicios para inyectar 
 * variables de contexto (ID de usuario e IP) directamente en la sesión de base de datos.
 * 
 * Utiliza SET LOCAL para que estas variables solo existan durante la transacción actual.
 * Funciona en conjunto con @EnableTransactionManagement(order = 0) en la clase principal
 * para garantizar que este aspecto se ejecute por dentro del inicio de la transacción 
 * de Spring y así compartir la misma conexión a la base de datos.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditoriaAspect {

    private final JdbcTemplate jdbcTemplate;

    // Intercepta todos los métodos de los servicios
    @Pointcut("execution(* com.fides.sienep.service.*.*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object setPostgresVariables(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String ip = null;
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ip = request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener la IP del request para la auditoría: {}", e.getMessage());
        }

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                // En CustomUsuarioDetailsService el username es el idUsuario
                String idUsuarioStr = auth.getName();
                
                // Validar que el ID sea numérico para evitar inyección SQL
                if (idUsuarioStr != null && idUsuarioStr.matches("\\d+")) {
                    jdbcTemplate.execute("SET LOCAL sienep.usuario_actual = '" + idUsuarioStr + "'");
                } else {
                    log.warn("El ID de usuario '{}' no es numérico, no se seteará en PostgreSQL.", idUsuarioStr);
                }
                
                // Setear IP si existe y validar formato básico
                if (ip != null && ip.matches("^[a-zA-Z0-9.:]+$")) {
                    jdbcTemplate.execute("SET LOCAL sienep.ip_actual = '" + ip + "'");
                } else if (ip != null) {
                    log.warn("La IP '{}' tiene formato inválido, no se seteará en PostgreSQL.", ip);
                }
            } catch (Exception e) {
                log.warn("Error al setear variables de auditoría (SET LOCAL) en PostgreSQL: {}", e.getMessage());
            }
        }
        
        return joinPoint.proceed();
    }
}
