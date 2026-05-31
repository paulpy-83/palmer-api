package com.palmar.palmer.api.service;

import com.palmar.palmer.api.dto.ArticuloStockViewDTO;
import com.palmar.palmer.api.dto.OpcionesFiltroDTO;
import com.palmar.palmer.api.mapper.ArticuloStockViewMapper;
import com.palmar.palmer.api.repository.ArticuloStockViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticuloStockViewService {

    private final ArticuloStockViewRepository repository;

    public Page<ArticuloStockViewDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(ArticuloStockViewMapper::toDTO);
    }

    public Page<ArticuloStockViewDTO> findByArticulo(String codArticulo, Pageable pageable) {
        return repository.findByCodArticulo(codArticulo, pageable).map(ArticuloStockViewMapper::toDTO);
    }

    public Page<ArticuloStockViewDTO> findByFamilia(String codFamilia, Pageable pageable) {
        return repository.findByCodFamilia(codFamilia, pageable).map(ArticuloStockViewMapper::toDTO);
    }

    public Page<ArticuloStockViewDTO> findByLinea(String codLinea, Pageable pageable) {
        return repository.findByCodLinea(codLinea, pageable).map(ArticuloStockViewMapper::toDTO);
    }

    public Page<ArticuloStockViewDTO> findByNombreArticulo(String nombre, Pageable pageable) {
        return repository.findByNombreArticulo(escapeLike(nombre), pageable).map(ArticuloStockViewMapper::toDTO);
    }

    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    @Cacheable("opciones-filtro")
    public OpcionesFiltroDTO getOpciones() {
        return new OpcionesFiltroDTO(
                repository.findFamiliasDistinct(),
                repository.findLineasDistinct()
        );
    }
}
