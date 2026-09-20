package com.fides.sienep.security;

import com.fides.sienep.model.EstadoLogico;
import com.fides.sienep.utils.JwtTokenUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final CustomUsuarioDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            try {
                username = jwtTokenUtil.extractUsername(jwt);
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("Token JWT inválido o vencido: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o vencido");
                return;
            }
        }

        if (username != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                log.warn("Usuario no encontrado para el token: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
                return;
            }

            if (jwtTokenUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                log.warn("Token no válido para el usuario: {}", username);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o vencido");
                return;
            }
        }

        // Un usuario PENDIENTE_DE_ACTIVACION (cuenta nueva o con contraseña temporal
        // reseteada por un admin) tiene las mismas GrantedAuthority que uno ACTIVO —
        // sin este chequeo podría seguir usando la API con normalidad sin nunca elegir
        // una contraseña propia. Se lee del SecurityContextHolder (no de la variable
        // local userDetails, que solo existe en la rama de arriba) para cubrir también
        // el caso de una autenticación ya presente en el contexto.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof SienepUserDetails sienepUserDetails
                && EstadoLogico.PENDIENTE_DE_ACTIVACION.equals(sienepUserDetails.getEstado())
                && !esRutaPermitidaPendienteActivacion(request)) {
            log.warn("Usuario id={} en PENDIENTE_DE_ACTIVACION intentó acceder a {} {}",
                    sienepUserDetails.getUsername(), request.getMethod(), request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Debe cambiar su contraseña temporal antes de continuar.");
            return;
        }

        chain.doFilter(request, response);
    }

    private static final String PATRON_CAMBIO_CONTRASENIA = "^/(funcionarios|estudiantes)/\\d+/contrasenia$";

    // Whitelist mínima para un usuario PENDIENTE_DE_ACTIVACION: ver su propia
    // identidad, cerrar sesión, y cambiar su propia contraseña (que es lo único que
    // lo saca de este estado, en FuncionarioService/EstudianteService.cambiarContrasenia()).
    // Nota: el reset admin (POST .../contrasenia, distinto endpoint que comparte el
    // mismo path) queda deliberadamente afuera — no es "el usuario cambiando la suya".
    private boolean esRutaPermitidaPendienteActivacion(HttpServletRequest request) {
        String metodo = request.getMethod();
        String path = request.getRequestURI();

        if ("GET".equals(metodo) && "/auth/me".equals(path)) {
            return true;
        }
        if ("POST".equals(metodo) && "/auth/logout".equals(path)) {
            return true;
        }
        return "PATCH".equals(metodo) && path.matches(PATRON_CAMBIO_CONTRASENIA);
    }
}