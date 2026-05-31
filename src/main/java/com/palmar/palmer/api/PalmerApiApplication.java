package com.palmar.palmer.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PalmerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PalmerApiApplication.class, args);
    }

}

