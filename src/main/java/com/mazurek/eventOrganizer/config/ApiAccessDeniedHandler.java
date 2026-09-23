package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiErrorResponseWriter responseWriter;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        responseWriter.write(response, new ErrorMessageDto(
                HttpStatus.FORBIDDEN.value(),
                ApiErrorCode.ACCESS_DENIED,
                "You do not have permission to access this resource."
        ));
    }
}
