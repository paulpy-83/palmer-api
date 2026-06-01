package com.palmar.palmer.api.dto;

import java.util.List;

public record LoginResponseDTO(String token, String username, List<String> roles) {}
