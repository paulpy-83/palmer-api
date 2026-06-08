package com.palmar.palmer.api.repository;

import com.palmar.palmer.api.dto.CodigoDescDTO;
import com.palmar.palmer.api.entity.PalmerDashboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PalmerDashboardRepository extends JpaRepository<PalmerDashboard, String> {

    // ── Filtros ──────────────────────────────────────────────────────────────

    Page<PalmerDashboard> findByCodFamilia(String codFamilia, Pageable pageable);

    Page<PalmerDashboard> findByCodLinea(String codLinea, Pageable pageable);

    // ── Búsqueda unificada (código, barra, descripción) ──────────────────────

    @Query("SELECT a FROM PalmerDashboard a " +
           "WHERE UPPER(a.articuloDes) LIKE UPPER(CONCAT('%', :q, '%')) ESCAPE '!'")
    Page<PalmerDashboard> searchByDescripcion(@Param("q") String q, Pageable pageable);

    Page<PalmerDashboard> findByCodArticulo(String codArticulo, Pageable pageable);

    Page<PalmerDashboard> findByCodBarra(String codBarra, Pageable pageable);

    @Query("SELECT a FROM PalmerDashboard a " +
           "WHERE UPPER(a.articuloDes) LIKE UPPER(CONCAT('%', :q, '%')) ESCAPE '!' " +
           "   OR UPPER(a.codArticulo) LIKE UPPER(CONCAT('%', :q, '%')) ESCAPE '!' " +
           "   OR UPPER(a.codBarra)    LIKE UPPER(CONCAT('%', :q, '%')) ESCAPE '!'")
    Page<PalmerDashboard> search(@Param("q") String q, Pageable pageable);

    // ── Opciones de filtro (familias y líneas distintas) ─────────────────────

    @Query("SELECT DISTINCT new com.palmar.palmer.api.dto.CodigoDescDTO(a.codFamilia, a.familiaDes) " +
           "FROM PalmerDashboard a WHERE a.codFamilia IS NOT NULL AND a.familiaDes IS NOT NULL " +
           "ORDER BY a.familiaDes ASC")
    List<CodigoDescDTO> findFamiliasDistinct();

    @Query("SELECT DISTINCT new com.palmar.palmer.api.dto.CodigoDescDTO(a.codLinea, a.lineaDes) " +
           "FROM PalmerDashboard a WHERE a.codLinea IS NOT NULL AND a.lineaDes IS NOT NULL " +
           "ORDER BY a.lineaDes ASC")
    List<CodigoDescDTO> findLineasDistinct();
}
