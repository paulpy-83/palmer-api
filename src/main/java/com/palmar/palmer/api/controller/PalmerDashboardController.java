package com.palmar.palmer.api.controller;

import com.palmar.palmer.api.dto.OpcionesFiltroDTO;
import com.palmar.palmer.api.dto.PalmerDashboardDTO;
import com.palmar.palmer.api.service.PalmerDashboardService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v2/stock")
@RequiredArgsConstructor
@Validated
public class PalmerDashboardController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final PalmerDashboardService service;

    @GetMapping
    public Page<PalmerDashboardDTO> findAll(
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "codArticuloNumero", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/familia/{codFamilia}")
    public Page<PalmerDashboardDTO> findByFamilia(
            @PathVariable @NotBlank @Size(max = 5) String codFamilia,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "codArticuloNumero", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findByFamilia(codFamilia, pageable);
    }

    @GetMapping("/linea/{codLinea}")
    public Page<PalmerDashboardDTO> findByLinea(
            @PathVariable @NotBlank @Size(max = 5) String codLinea,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "codArticuloNumero", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findByLinea(codLinea, pageable);
    }

    @GetMapping("/search")
    public Page<PalmerDashboardDTO> search(
            @RequestParam @NotBlank @Size(min = 1, max = 100) String q,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "codArticuloNumero", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.search(q, pageable);
    }

    @GetMapping("/opciones")
    public ResponseEntity<OpcionesFiltroDTO> getOpciones() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(6, TimeUnit.HOURS))
                .body(service.getOpciones());
    }
}
