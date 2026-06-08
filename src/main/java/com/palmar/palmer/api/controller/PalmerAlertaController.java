package com.palmar.palmer.api.controller;

import com.palmar.palmer.api.dto.PalmerAlertaDTO;
import com.palmar.palmer.api.service.PalmerAlertaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v2/alertas")
@RequiredArgsConstructor
@Validated
public class PalmerAlertaController {

    private final PalmerAlertaService service;

    @GetMapping
    public ResponseEntity<List<PalmerAlertaDTO>> getAlertas() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
                .body(service.findAll());
    }
}
