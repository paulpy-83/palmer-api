package com.palmar.palmer.api.service;

import com.palmar.palmer.api.dto.PalmerDetalleDTO;
import com.palmar.palmer.api.dto.PalmerStockSucursalDTO;
import com.palmar.palmer.api.exception.ResourceNotFoundException;
import com.palmar.palmer.api.mapper.PalmerDetalleMapper;
import com.palmar.palmer.api.mapper.PalmerStockSucursalMapper;
import com.palmar.palmer.api.repository.PalmerDetalleRepository;
import com.palmar.palmer.api.repository.PalmerStockSucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PalmerArticuloService {

    private final PalmerDetalleRepository detalleRepository;
    private final PalmerStockSucursalRepository sucursalRepository;

    /** Detalle completo del artículo (header + imagen + rentabilidad) */
    public PalmerDetalleDTO findDetalle(String codArticulo) {
        return detalleRepository.findByCodArticulo(codArticulo)
                .map(PalmerDetalleMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Artículo", "codArticulo", codArticulo));
    }

    /** Stock por sucursal para el tab "Por Sucursal" */
    public List<PalmerStockSucursalDTO> findSucursales(String codArticulo) {
        return sucursalRepository
                .findByCodArticuloOrderByCodSucursalAsc(codArticulo)
                .stream()
                .map(PalmerStockSucursalMapper::toDTO)
                .toList();
    }

    /** Nombre del archivo de imagen en Samba — lanzar 404 si no existe */
    public String findImagenFilename(String codArticulo) {
        return detalleRepository.findByCodArticulo(codArticulo)
                .map(e -> e.getImagen())
                .filter(img -> img != null && !img.isBlank())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Imagen", "codArticulo", codArticulo));
    }
}
