package com.palmar.palmer.api.repository;

import com.palmar.palmer.api.dto.CodigoDescDTO;
import com.palmar.palmer.api.entity.ArticuloStockView;
import com.palmar.palmer.api.entity.ArticuloStockViewId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticuloStockViewRepository extends JpaRepository<ArticuloStockView, ArticuloStockViewId> {

    Optional<ArticuloStockView> findFirstByCodArticuloOrderByCodSucursalAsc(String codArticulo);

    @Query("SELECT DISTINCT new com.palmar.palmer.api.dto.CodigoDescDTO(a.codFamilia, a.familiaDes) " +
           "FROM ArticuloStockView a WHERE a.codFamilia IS NOT NULL AND a.familiaDes IS NOT NULL " +
           "ORDER BY a.familiaDes ASC")
    List<CodigoDescDTO> findFamiliasDistinct();

    @Query("SELECT DISTINCT new com.palmar.palmer.api.dto.CodigoDescDTO(a.codLinea, a.lineaDes) " +
           "FROM ArticuloStockView a WHERE a.codLinea IS NOT NULL AND a.lineaDes IS NOT NULL " +
           "ORDER BY a.lineaDes ASC")
    List<CodigoDescDTO> findLineasDistinct();

    Page<ArticuloStockView> findByCodArticulo(String codArticulo, Pageable pageable);

    Page<ArticuloStockView> findByCodFamilia(String codFamilia, Pageable pageable);

    Page<ArticuloStockView> findByCodLinea(String codLinea, Pageable pageable);

    @Query("SELECT a FROM ArticuloStockView a WHERE UPPER(a.articuloDes) LIKE UPPER(CONCAT('%', :nombre, '%')) ESCAPE '!'")
    Page<ArticuloStockView> findByNombreArticulo(@Param("nombre") String nombre, Pageable pageable);
}
