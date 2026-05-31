package com.palmar.palmer.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
    public static ErrorResponseDTO of(int status, String error, String message, String path) {
        return new ErrorResponseDTO(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponseDTO of(int status, String error, String message, String path, List<String> details) {
        return new ErrorResponseDTO(Instant.now(), status, error, message, path, details);
    }
}
