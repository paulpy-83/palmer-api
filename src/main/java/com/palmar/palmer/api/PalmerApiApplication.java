package com.palmar.palmer.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableCaching
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class PalmerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PalmerApiApplication.class, args);
    }

}

