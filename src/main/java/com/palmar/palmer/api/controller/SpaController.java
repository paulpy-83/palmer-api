package com.palmar.palmer.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Redirige todas las rutas que no son /api/** ni archivos estáticos a index.html,
 * permitiendo que React Router maneje el routing del lado del cliente.
 * Ejemplos: /login, /article/123, /alerts → sirve index.html
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
        "/",
        "/login",
        "/alerts",
        "/article/**"
    })
    public String spa() {
        return "forward:/index.html";
    }
}
