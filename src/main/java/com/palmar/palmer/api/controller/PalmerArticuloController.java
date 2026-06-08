package com.palmar.palmer.api.controller;

import com.palmar.palmer.api.dto.PalmerDetalleDTO;
import com.palmar.palmer.api.dto.PalmerStockSucursalDTO;
import com.palmar.palmer.api.exception.ResourceNotFoundException;
import com.palmar.palmer.api.service.PalmerArticuloService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

@RestController
@RequestMapping("/api/v2/articulos")
@RequiredArgsConstructor
@Validated
public class PalmerArticuloController {

    @Value("${cache.imagenes.ttl-horas:24}")
    private int cacheTtlHoras;

    private final PalmerArticuloService articuloService;
    private final SambaService sambaService;

    /** Detalle completo del artículo (header + imagen + rentabilidad) */
    @GetMapping("/{codArticulo}")
    public PalmerDetalleDTO getDetalle(
            @PathVariable @NotBlank @Size(max = 5) String codArticulo) {
        return articuloService.findDetalle(codArticulo);
    }

    /** Stock por sucursal — tab "Por Sucursal" */
    @GetMapping("/{codArticulo}/sucursales")
    public List<PalmerStockSucursalDTO> getSucursales(
            @PathVariable @NotBlank @Size(max = 5) String codArticulo) {
        return articuloService.findSucursales(codArticulo);
    }

    /** Imagen del artículo desde Samba con caché HTTP (ETag + Cache-Control) */
    @GetMapping("/{codArticulo}/imagen")
    public ResponseEntity<byte[]> getImagen(
            @PathVariable @NotBlank @Size(max = 5) String codArticulo,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {

        String filename = articuloService.findImagenFilename(codArticulo);

        byte[] bytes = sambaService.fetchImage(filename)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen", "archivo", filename));

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
