package com.palmar.palmer.api.service;

import com.palmar.palmer.api.dto.OpcionesFiltroDTO;
import com.palmar.palmer.api.dto.PalmerDashboardDTO;
import com.palmar.palmer.api.entity.PalmerDashboard;
import com.palmar.palmer.api.mapper.PalmerDashboardMapper;
import com.palmar.palmer.api.repository.PalmerDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PalmerDashboardService {

    private final PalmerDashboardRepository repository;

    public Page<PalmerDashboardDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(PalmerDashboardMapper::toDTO);
    }

    public Page<PalmerDashboardDTO> findByFamilia(String codFamilia, Pageable pageable) {
        return repository.findByCodFamilia(codFamilia, pageable).map(PalmerDashboardMapper::toDTO);
    }

    public Page<PalmerDashboardDTO> findByLinea(String codLinea, Pageable pageable) {
        return repository.findByCodLinea(codLinea, pageable).map(PalmerDashboardMapper::toDTO);
    }

    public Page<PalmerDashboardDTO> search(String q, Pageable pageable) {
        String trimmed = q.trim();

        // Solo dígitos: detectar si es código de artículo o código de barras
        if (trimmed.matches("\\d+")) {
            if (trimmed.length() <= 5) {
                // Código de artículo — búsqueda exacta
                Page<PalmerDashboard> result = repository.findByCodArticulo(trimmed, pageable);
                if (!result.isEmpty()) return result.map(PalmerDashboardMapper::toDTO);
            } else {
                // Código de barras — búsqueda exacta
                Page<PalmerDashboard> result = repository.findByCodBarra(trimmed, pageable);
                if (!result.isEmpty()) return result.map(PalmerDashboardMapper::toDTO);
            }
        }

        // Contiene letras: buscar solo por descripción
        if (trimmed.matches(".*[a-zA-Zà-ÿ].*")) {
            return repository.searchByDescripcion(escapeLike(trimmed), pageable)
                             .map(PalmerDashboardMapper::toDTO);
        }

        // Fallback: OR en los tres campos
        return repository.search(escapeLike(trimmed), pageable).map(PalmerDashboardMapper::toDTO);
    }

    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    @Cacheable(value = "opciones-filtro", sync = true)
    public OpcionesFiltroDTO getOpciones() {
        return new OpcionesFiltroDTO(
                repository.findFamiliasDistinct(),
                repository.findLineasDistinct()
        );
    }
}
