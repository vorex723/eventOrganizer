package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String INVALID_ACCESS_TOKEN_ATTRIBUTE =
            ApiAuthenticationEntryPoint.class.getName() + ".invalidAccessToken";

    private final ApiErrorResponseWriter responseWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException {
        boolean invalidToken = Boolean.TRUE.equals(request.getAttribute(INVALID_ACCESS_TOKEN_ATTRIBUTE));
        responseWriter.write(response, new ErrorMessageDto(
                HttpStatus.UNAUTHORIZED.value(),
                invalidToken ? ApiErrorCode.INVALID_ACCESS_TOKEN : ApiErrorCode.AUTHENTICATION_REQUIRED,
                invalidToken ? "The access token is invalid or expired." : "Authentication is required to access this resource."
        ));
    }
}
