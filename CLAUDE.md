# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Tech Stack

- **Java 21**, Spring Boot 4.0.6
- **Build tool:** Maven 3.9.15 via wrapper (`mvnw` / `mvnw.cmd`)
- **Root package:** `com.palmar.palmer.api`

## Common Commands

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=PalmerApiApplicationTests

# Dependency tree
./mvnw dependency:tree
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Architecture

```
src/main/java/com/palmar/palmer/api/
├── PalmerApiApplication.java          — entry point (@SpringBootApplication @EnableCaching)
├── config/
│   ├── CacheConfig.java               — Caffeine CacheManager (caches: imagenes, opciones-filtro)
│   └── SambaProperties.java           — @ConfigurationProperties(prefix="samba")
├── controller/
│   ├── AuthController.java            — POST /api/auth/login
│   ├── ArticuloStockViewController.java — GET /api/stock/** (paginado) + GET /api/stock/opciones
│   └── ArticuloImageController.java   — GET /api/articulos/{codArticulo}/imagen
├── dto/
│   ├── LoginRequestDTO.java
│   ├── LoginResponseDTO.java
│   ├── ArticuloStockViewDTO.java
│   ├── CodigoDescDTO.java             — { codigo, descripcion } para opciones de filtro
│   ├── OpcionesFiltroDTO.java         — { familias, lineas } respuesta de /api/stock/opciones
│   └── ErrorResponseDTO.java
├── entity/
│   ├── ArticuloStockView.java         — @Immutable, mapea la vista VW_ARTICULOS_REST_API
│   ├── ArticuloStockViewId.java       — clave compuesta (codArticulo + codSucursal)
│   └── Usuario.java                   — @Immutable, mapea la tabla USUARIOS (autenticación)
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── mapper/
│   └── ArticuloStockViewMapper.java
├── repository/
│   ├── ArticuloStockViewRepository.java — JpaRepository + JPQL para búsqueda y opciones de filtro
│   └── UsuarioRepository.java           — JpaRepository<Usuario>, findByCodUsuarioAndEstado
├── security/
│   ├── JwtUtil.java                   — generación y validación de tokens JWT (jjwt 0.12.6); claim "roles"
│   ├── JwtAuthFilter.java             — OncePerRequestFilter para validar Bearer token
│   ├── AppUserDetailsService.java     — UserDetailsService que carga usuarios desde Oracle (USUARIOS)
│   └── SecurityConfig.java           — filtro STATELESS, CORS, PasswordEncoder (uppercase)
└── service/
    ├── ArticuloStockViewService.java  — @Transactional(readOnly=true), orquesta repo + mapper
    └── SambaService.java              — conexión SMB2 persistente con pool (ReentrantLock), caché Caffeine
```

## REST API Endpoints

### Auth (pública)
| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/auth/login` | Retorna JWT token + `username` + `roles` |

### Imágenes (requiere `Authorization: Bearer <token>`)
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/articulos/{codArticulo}/imagen` | Retorna imagen JPEG del artículo desde servidor Samba |

> Responde con `Cache-Control: public, max-age=86400` y `ETag` basado en el nombre de archivo. Soporta `If-None-Match` → `304 Not Modified`. Retorna `404` si el artículo no tiene imagen o el archivo no existe en el share.

### Stock (requiere `Authorization: Bearer <token>`)
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/stock` | Listado paginado completo |
| GET | `/api/stock/articulo/{codArticulo}` | Filtrar por artículo (todas las sucursales) |
| GET | `/api/stock/familia/{codFamilia}` | Filtrar por familia |
| GET | `/api/stock/linea/{codLinea}` | Filtrar por línea |
| GET | `/api/stock/search?nombre=` | Búsqueda por nombre (LIKE, min 2 chars) |
| GET | `/api/stock/opciones` | Familias y líneas distintas para filtros del frontend |

Paginación por defecto: `size=20`, `sort=codArticulo,ASC`. Soporta params `?page=&size=&sort=`.

## DTOs

### LoginResponseDTO
Respuesta de `POST /api/auth/login`.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `token` | String | JWT Bearer token |
| `username` | String | Nombre de usuario autenticado |
| `roles` | `List<String>` | Roles del usuario (ej. `["ROLE_USER"]`) |

### ArticuloStockViewDTO
Respuesta de endpoints de detalle y listado paginado (`/api/stock/**`).

| Campo | Tipo | Columna vista |
|-------|------|---------------|
| `codArticulo` | String | COD_ARTICULO |
| `articuloDes` | String | DESC_ARTICULO |
| `codSucursal` | String | COD_SUCURSAL |
| `sucursalDes` | String | DESC_SUCURSAL |
| `codEmpresa` | String | COD_EMPRESA |
| `codMarca` | String | COD_MARCA |
| `descMarca` | String | DESC_MARCA |
| `codFamilia` | String | COD_FAMILIA |
| `familiaDes` | String | DESC_FAMILIA |
| `codLinea` | String | COD_LINEA |
| `lineaDes` | String | DESC_LINEA |
| `codUnidadMedida` | String | COD_UNIDAD_MEDIDA |
| `descUnidadMedida` | String | DESC_UNIDAD_MEDIDA |
| `costoUltimo` | BigDecimal | COSTO_ULTIMO |
| `precioBase` | BigDecimal | PRECIO_BASE |
| `nroLote` | String | NRO_LOTE |
| `cantMinima` | BigDecimal | CANT_MINIMA |
| `cantDispon` | BigDecimal | CANT_DISPON |
| `codPrecioFijo` | String | COD_PRECIO_FIJO |
| `descListaPrecio` | String | DESC_LISTA_PRECIO |
| `fecVigencia` | LocalDate | FEC_VIGENCIA |
| `precioVenta` | BigDecimal | PRECIO_VENTA |
| `codBarra` | String | COD_BARRA |

> `precioVenta` y `descListaPrecio` son informativos: Hibernate devuelve la primera fila que Oracle resuelva para el par `codArticulo + codSucursal` (sin filtrar por lista de precio).

### OpcionesFiltroDTO
Respuesta de `GET /api/stock/opciones`. Listas de valores distintos para poblar los filtros del frontend.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `familias` | `List<CodigoDescDTO>` | Familias distintas, ordenadas por descripción |
| `lineas` | `List<CodigoDescDTO>` | Líneas distintas, ordenadas por descripción |

`CodigoDescDTO`: `{ codigo: String, descripcion: String }`

## Dependencies

| Artefacto | Versión | Propósito |
|-----------|---------|-----------|
| `spring-boot-starter-web` | BOM 4.0.6 | REST controllers |
| `spring-boot-starter-data-jpa` | BOM | JPA + Hibernate 7 |
| `spring-boot-starter-security` | BOM | Seguridad HTTP |
| `spring-boot-starter-validation` | BOM | `@Valid`, constraints |
| `spring-boot-starter-actuator` | BOM | Health/info endpoints |
| `spring-boot-starter-cache` | BOM | Abstracción de caché (`@Cacheable`) |
| `caffeine` | BOM | Implementación de caché en memoria (LRU, TTL) |
| `ojdbc11` | 23.7.0.25.01 | Driver Oracle 12c+ |
| `jjwt-api/impl/jackson` | 0.12.6 | JWT |
| `dotenv-java` | 3.0.0 | Carga `.env` en local |
| `smbj` | 0.13.0 | Cliente SMB2/Samba para acceso a imágenes |
| `lombok` | BOM | Generación de código |

## Persistence

- **ORM:** Spring Data JPA + Hibernate 7, `OracleDialect`
- **Pool:** HikariCP — dev: max 5 conex., prod: max 20 conex.
- **fetch_size:** dev: 100, prod: 500 (reduce round-trips JDBC con Oracle)
- **DDL:** `ddl-auto: none` — schema gestionado con scripts en `src/main/resources/db/oracle/`
- **Vista Oracle:** `VW_ARTICULOS_REST_API` — entidad `@Immutable` con clave compuesta (`codArticulo + codSucursal`)

## Cache (Caffeine)

| Cache | Key | TTL | Max entradas | Propósito |
|-------|-----|-----|--------------|-----------|
| `imagenes` | filename | 24h | 500 | Bytes de imágenes descargadas desde Samba |
| `opciones-filtro` | — | 6h | 1 | Familias y líneas distintas para filtros |

TTL y tamaño fijos en `application.yaml` (`cache.imagenes.*`, `cache.opciones.*`).

## Security

- Autenticación **stateless** con JWT (Bearer token)
- Usuarios cargados desde la tabla Oracle `USUARIOS` via `AppUserDetailsService`
  - Solo usuarios con `ESTADO = 'A'` (activos) pueden autenticarse
  - El rol se toma de la columna `COD_GRUPO` → `SimpleGrantedAuthority(codGrupo)`
  - Contraseñas almacenadas en mayúsculas en `CLAVE_AUTORIZACION`; `PasswordEncoder` convierte a uppercase antes de comparar
- El JWT incluye `subject` (username) y el claim `"roles"` (lista de authorities)
- Ruta pública: `/api/auth/**`; el resto requiere token válido
- CORS configurable via `CORS_ALLOWED_ORIGINS` (variable de entorno, requerida)

## Compresión HTTP

Habilitada en todos los perfiles (`application.yaml` base):
- `min-response-size: 1024` — no comprime respuestas < 1 KB
- MIME types comprimidos: `application/json`, `application/javascript`, `text/css`, `text/plain`
- Las imágenes JPEG no se comprimen (ya están comprimidas por naturaleza)

## Resources

```
src/main/resources/
├── application.yaml          — valores fijos (JWT 1h, caché TTL/max), compresión, credenciales via ${...}
├── application-dev.yaml      — infraestructura dev (DB url, schema INV, Samba 10.30.1.3), pool reducido, SQL debug
├── application-prod.yaml     — infraestructura prod via ${ENV_VAR}, pool ampliado, fetch_size=500, logs reducidos
└── db/oracle/
    └── V1__create_persona.sql
```

### Estrategia de configuración

| Tipo | Dónde |
|------|-------|
| Secretos (`DB_PASSWORD`, `JWT_SECRET`, `SAMBA_PASSWORD`, etc.) | `.env` (nunca commiteado) |
| Infraestructura por ambiente (DB url, schema, Samba host/share) | `application-{dev\|prod}.yaml` |
| Valores fijos no sensibles (JWT expiration, caché TTL/max) | `application.yaml` base |

## Environment Variables

Solo secretos y control de entorno. Los valores de infraestructura (DB url, schema, Samba host/share/domain) se definen en los YAMLs de perfil.

| Variable | Descripción |
|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | Perfil activo (`dev` \| `prod`) |
| `DB_USERNAME` | Usuario Oracle |
| `DB_PASSWORD` | Contraseña Oracle |
| `JWT_SECRET` | Clave secreta JWT (HS256) — generar con `openssl rand -base64 32` |
| `CORS_ALLOWED_ORIGINS` | Orígenes CORS separados por coma (ej. `http://localhost:5173`) |
| `SAMBA_USER` | Usuario para autenticar en el share SMB |
| `SAMBA_PASSWORD` | Contraseña del usuario Samba |

En desarrollo local definir estas variables en `.env` en la raíz del proyecto (cargado por `dotenv-java`).

En producción `DB_HOST`, `DB_PORT`, `DB_SERVICE`, `DB_SCHEMA`, `SAMBA_HOST`, `SAMBA_SHARE`, `SAMBA_FOLDER`, `SAMBA_DOMAIN` deben setearse como variables de entorno reales del servidor (referenciadas en `application-prod.yaml`).

## Samba / Imágenes

- El endpoint `GET /api/articulos/{codArticulo}/imagen` obtiene el nombre de archivo desde la columna `IMAGEN` de la vista Oracle.
- **Pool de conexión:** `SambaService` mantiene una conexión SMB2 persistente (`Connection + Session + DiskShare`) reutilizada entre requests. Se protege con `ReentrantLock` para concurrencia. Reconecta automáticamente si la conexión cae (`ensureConnected()`). Se abre en `@PostConstruct` y se cierra en `@PreDestroy`.
- **Caché en memoria:** los bytes de la imagen se guardan en Caffeine (`@Cacheable("imagenes")`). Cache miss: ~50–100ms (sin handshake SMB). Cache hit: < 1ms (RAM).
- **Caché HTTP:** responde con `Cache-Control: public, max-age=86400` y `ETag` (hash del filename). Soporta `If-None-Match` → `304 Not Modified` sin body.
- Si la columna `IMAGEN` está vacía o el archivo no existe en el share, retorna `404 Not Found`.
- Requiere que la vista `VW_ARTICULOS_REST_API` incluya las columnas `IMAGEN` y `COD_BARRA`.
