package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorMessageDto error) throws IOException {
        response.setStatus(error.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
