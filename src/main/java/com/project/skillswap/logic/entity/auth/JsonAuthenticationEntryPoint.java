package com.project.skillswap.logic.entity.auth;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    //#region Dependencies
    private final ObjectMapper mapper = new ObjectMapper();
    //#endregion

    //#region EntryPoint
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        String message = "error de autenticación";

        Throwable cause = authException.getCause();
        if (cause instanceof ExpiredJwtException) {
            message = "sesión expirada";
        } else if (cause instanceof MalformedJwtException
                || cause instanceof SignatureException
                || cause instanceof UnsupportedJwtException
                || cause instanceof IllegalArgumentException) {
            message = "token inválido";
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getWriter(), Map.of("error", message));
    }
    //#endregion
}
