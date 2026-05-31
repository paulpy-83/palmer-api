package com.palmar.palmer.api.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s no encontrado: %s = '%s'", resource, field, value));
    }
}
