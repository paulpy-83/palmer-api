package com.palmar.palmer.api.controller;

import com.palmar.palmer.api.entity.ArticuloStockView;
import com.palmar.palmer.api.exception.ResourceNotFoundException;
import com.palmar.palmer.api.repository.ArticuloStockViewRepository;
import com.palmar.palmer.api.service.SambaService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

@RestController
@RequestMapping("/api/articulos")
@RequiredArgsConstructor
@Validated
public class ArticuloImageController {

    @Value("${cache.imagenes.ttl-horas:24}")
    private int cacheTtlHoras;

    private final ArticuloStockViewRepository repository;
    private final SambaService sambaService;

    @GetMapping("/{codArticulo}/imagen")
    public ResponseEntity<byte[]> getImagen(
            @PathVariable @NotBlank @Size(max = 5) String codArticulo,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {

        String filename = repository.findFirstByCodArticuloOrderByCodSucursalAsc(codArticulo)
                .map(ArticuloStockView::getImagen)
                .filter(img -> img != null && !img.isBlank())
                .orElseThrow(() -> new ResourceNotFoundException("Imagen", "codArticulo", codArticulo));

        byte[] bytes = sambaService.fetchImage(filename)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen", "archivo", filename));

        // ETag basado en el contenido real — cambia si el archivo es reemplazado en Samba
        String etag = "\"" + computeEtag(bytes) + "\"";

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(cacheTtlHoras, TimeUnit.HOURS).cachePublic())
                .eTag(etag)
                .body(bytes);
    }

    private static String computeEtag(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return Long.toHexString(crc.getValue());
    }
}
