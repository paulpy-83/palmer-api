package com.palmar.palmer.api.service;

import com.palmar.palmer.api.dto.PalmerAlertaDTO;
import com.palmar.palmer.api.mapper.PalmerAlertaMapper;
import com.palmar.palmer.api.repository.PalmerAlertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PalmerAlertaService {

    private final PalmerAlertaRepository repository;

    /**
     * Todas las alertas globales — sin paginación, Oracle ya filtró con HAVING.
     * Cacheado 5 minutos (configurable via cache.alertas.ttl-minutos).
     */
    @Cacheable(value = "alertas", sync = true)
    public List<PalmerAlertaDTO> findAll() {
        return repository.findAllOrdered()
                .stream()
                .map(PalmerAlertaMapper::toDTO)
                .toList();
    }
}
