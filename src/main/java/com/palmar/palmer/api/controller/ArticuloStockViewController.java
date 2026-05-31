package com.palmar.palmer.api.controller;

import com.palmar.palmer.api.dto.ArticuloStockViewDTO;
import com.palmar.palmer.api.dto.OpcionesFiltroDTO;
import com.palmar.palmer.api.service.ArticuloStockViewService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Validated
public class ArticuloStockViewController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ArticuloStockViewService service;

    @GetMapping
    public Page<ArticuloStockViewDTO> findAll(
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "codArticulo", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/articulo/{codArticulo}")
    public Page<ArticuloStockViewDTO> findByArticulo(
            @PathVariable @NotBlank @Size(max = 5) String codArticulo,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "codSucursal", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findByArticulo(codArticulo, pageable);
    }

    @GetMapping("/familia/{codFamilia}")
    public Page<ArticuloStockViewDTO> findByFamilia(
            @PathVariable @NotBlank @Size(max = 5) String codFamilia,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "codArticulo", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findByFamilia(codFamilia, pageable);
    }

    @GetMapping("/linea/{codLinea}")
    public Page<ArticuloStockViewDTO> findByLinea(
            @PathVariable @NotBlank @Size(max = 5) String codLinea,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "codArticulo", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findByLinea(codLinea, pageable);
    }

    @GetMapping("/search")
    public Page<ArticuloStockViewDTO> searchByNombre(
            @RequestParam @NotBlank @Size(min = 2, max = 100) String nombre,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "codArticulo", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findByNombreArticulo(nombre, pageable);
    }

    @GetMapping("/opciones")
    public OpcionesFiltroDTO getOpciones() {
        return service.getOpciones();
    }
}
