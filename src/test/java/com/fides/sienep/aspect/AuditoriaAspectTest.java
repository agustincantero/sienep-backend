package com.fides.sienep.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaAspectTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private AuditoriaAspect auditoriaAspect;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private void mockRequestConIp(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null, List.of()));
    }

    @Test
    @DisplayName("IPv6 entre corchetes - Se guarda sin los corchetes")
    void setPostgresVariables_Ipv6EntreCorchetes_GuardaSinCorchetes() throws Throwable {
        mockRequestConIp("[2800:a4:2e6:d800:c83f:b0fa:92b3:ce0e]");

        auditoriaAspect.setPostgresVariables(joinPoint);

        verify(jdbcTemplate).execute(contains("2800:a4:2e6:d800:c83f:b0fa:92b3:ce0e"));
        verify(jdbcTemplate, never()).execute(contains("["));
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("IPv4 - Se guarda tal cual")
    void setPostgresVariables_Ipv4_GuardaTalCual() throws Throwable {
        mockRequestConIp("192.168.1.10");

        auditoriaAspect.setPostgresVariables(joinPoint);

        verify(jdbcTemplate).execute(contains("192.168.1.10"));
    }

    @Test
    @DisplayName("IP con formato inválido - No se ejecuta el SET LOCAL")
    void setPostgresVariables_IpInvalida_NoEjecutaSetLocal() throws Throwable {
        mockRequestConIp("192.168.1.10; DROP TABLE proyecto.auditoria;--");

        auditoriaAspect.setPostgresVariables(joinPoint);

        verify(jdbcTemplate, never()).execute(contains("ip_actual"));
        verify(joinPoint).proceed();
    }
}
