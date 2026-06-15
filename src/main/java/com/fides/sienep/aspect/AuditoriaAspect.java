package com.fides.sienep.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuditoriaAspect {

    private final JdbcTemplate jdbcTemplate;

    public AuditoriaAspect(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Intercepta todos los métodos de los servicios
    @Pointcut("execution(* com.fides.sienep.service.*.*(..))")
    public void serviceMethods() {}

    @Before("serviceMethods()")
    public void setPostgresVariables() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String ip = null;
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ip = request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Ignorar si no hay request context
        }

        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            try {
                // En CustomUsuarioDetailsService el username es el idUsuario
                String idUsuarioStr = auth.getName();
                
                // Setear usuario
                jdbcTemplate.execute("SET LOCAL sienep.usuario_actual = '" + idUsuarioStr + "'");
                
                // Setear IP si existe
                if (ip != null) {
                    jdbcTemplate.execute("SET LOCAL sienep.ip_actual = '" + ip + "'");
                }
            } catch (Exception e) {
                // Loggear o ignorar
            }
        }
    }
}
