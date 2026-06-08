package com.palmar.palmer.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Value("${cache.imagenes.ttl-horas:24}")
    private int imagenesTtlHoras;

    @Value("${cache.imagenes.max-entradas:500}")
    private int imagenesMaxEntradas;

    @Value("${cache.opciones.ttl-horas:6}")
    private int opcionesTtlHoras;

    @Value("${cache.alertas.ttl-minutos:5}")
    private int alertasTtlMinutos;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.registerCustomCache("imagenes",
            Caffeine.newBuilder()
                .expireAfterWrite(imagenesTtlHoras, TimeUnit.HOURS)
                .maximumSize(imagenesMaxEntradas)
                .recordStats()
                .build()
        );

        manager.registerCustomCache("opciones-filtro",
            Caffeine.newBuilder()
                .expireAfterWrite(opcionesTtlHoras, TimeUnit.HOURS)
                .maximumSize(1)
                .recordStats()
                .build()
        );

        manager.registerCustomCache("alertas",
            Caffeine.newBuilder()
                .expireAfterWrite(alertasTtlMinutos, TimeUnit.MINUTES)
                .maximumSize(1)   // resultado único — no varía por key
                .recordStats()
                .build()
        );

        return manager;
    }
}
