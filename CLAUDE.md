# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Tech Stack

- **Java 21**, Spring Boot 4.0.6
- **Build tool:** Maven 3.9.15 via wrapper (`mvnw` / `mvnw.cmd`)
- **Root package:** `com.palmar.palmer.api`

## Common Commands

```bash
# Build dev (copia .env → target/)
./mvnw clean package -DskipTests

# Build prod (build frontend + copia .env.prod → target/.env)
./mvnw clean package -Pprod -DskipTests

# Run
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Perfiles Maven

| Comando | Perfil | `.env` copiado | Frontend incluido |
|---------|--------|----------------|-------------------|
| `.\mvnw.cmd clean package` | `dev` (default) | `.env` | ❌ No |
| `.\mvnw.cmd clean package -Pprod` | `prod` | `.env.prod` → `.env` | ✅ Sí |

En prod el JAR sirve API REST + frontend estático desde `static/`.

## Architecture

```
src/main/java/com/palmar/palmer/api/
├── PalmerApiApplication.java   — @SpringBootApplication @EnableCaching
│                                 @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
├── config/
│   ├── CacheConfig.java        — Caffeine (imagenes, opciones-filtro, alertas)
│   └── SambaProperties.java    — @ConfigurationProperties(prefix="samba")
├── controller/
│   ├── AuthController.java           — POST /api/auth/login
│   ├── PalmerDashboardController.java — /api/v2/stock/**
│   ├── PalmerArticuloController.java  — /api/v2/articulos/**
│   ├── PalmerAlertaController.java    — /api/v2/alertas
│   └── SpaController.java             — SPA fallback → index.html
├── dto/
│   ├── LoginRequestDTO / LoginResponseDTO
│   ├── PalmerDashboardDTO      — cantDisponTotal, nroSucursales, nroCrit, nroBajo
│   ├── PalmerDetalleDTO        — detalle completo artículo
│   ├── PalmerStockSucursalDTO  — stock por sucursal con EstadoStock
│   ├── PalmerAlertaDTO         — alerta por (artículo × sucursal) con EstadoStock y TipoAlerta
│   ├── CodigoDescDTO / OpcionesFiltroDTO / ErrorResponseDTO
├── entity/
│   ├── EstadoStock              — enum: ok | bajo | critico
│   ├── TipoAlerta               — enum: reposicion | compra
│   ├── PalmerDashboard          — VW_PALMER_DASHBOARD
│   ├── PalmerDetalle            — VW_PALMER_DETALLE
│   ├── PalmerStockSucursal / PalmerStockSucursalId — VW_PALMER_STOCK_SUCURSAL
│   ├── PalmerAlerta             — VW_PALMER_ALERTAS (reutiliza PalmerStockSucursalId)
│   └── Usuario
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── mapper/
│   ├── PalmerDashboardMapper / PalmerDetalleMapper
│   ├── PalmerStockSucursalMapper / PalmerAlertaMapper
├── repository/
│   ├── PalmerDashboardRepository    — findAll paginado, filtros, búsqueda, opciones
│   ├── PalmerDetalleRepository      — findByCodArticulo → Optional
│   ├── PalmerStockSucursalRepository — findByCodArticuloOrderByCodSucursalAsc → List
│   ├── PalmerAlertaRepository       — findAllOrdered → List (críticos primero)
│   └── UsuarioRepository
├── security/
│   ├── JwtUtil / JwtAuthFilter / OracleAuthenticationProvider / SecurityConfig
│   └── AppUserDetailsService  — ⚠ código muerto (no invocado en el flujo actual)
└── service/
    ├── PalmerDashboardService   — findAll, filtros, search, getOpciones (@Cacheable)
    ├── PalmerArticuloService    — findDetalle + findSucursales + findImagenFilename
    ├── PalmerAlertaService      — findAll sin paginación (@Cacheable TTL 5 min), filtra nulls
    └── SambaService             — SMB2 persistente + Caffeine cache
```

## Vistas Oracle (VW_PALMER_*)

| Vista | Cardinalidad | Propósito |
|-------|-------------|-----------|
| `VW_PALMER_DASHBOARD` | 1 fila / artículo | Listado paginado — `GROUP BY`, badges precalculados |
| `VW_PALMER_DETALLE` | 1 fila / artículo | Detalle completo — header, imagen, rentabilidad |
| `VW_PALMER_STOCK_SUCURSAL` | 1 fila / (artículo × sucursal) | Tab "Por Sucursal" — `EstadoStock` precalculado |
| `VW_PALMER_ALERTAS` | Filas de alerta reposicion + fila compra por artículo | `UNION ALL` de dos bloques |

Todas las vistas usan `COALESCE(CANT_MINIMA, 5)` — el valor por defecto de stock mínimo
está definido en Oracle, no en el frontend.

`ESTADO_STOCK` en `VW_PALMER_STOCK_SUCURSAL` y `VW_PALMER_ALERTAS` retorna los valores
`'ok'`, `'bajo'`, `'critico'` que mapean directamente al enum `EstadoStock`.

### VW_PALMER_ALERTAS — Lógica de dos bloques (UNION ALL)

**Bloque 1 — reposicion:**
- Filas de tiendas (excluye sucursal `05`) con stock bajo/crítico
- Solo si el depósito `05` tiene `COALESCE(SUM(CANT_DISPON), 0) > 0` (subquery en `HAVING`)
- `TIPO_ALERTA = 'reposicion'`

**Bloque 2 — compra:**
- Una fila por artículo representando al depósito `05`
- Solo si `COALESCE(SUM(dep.CANT_DISPON), 0) <= 0` (subquery en `WHERE`)
- Sin `LEFT JOIN` a `ST_EXISTENCIA_ART` — `COD_SUCURSAL` hardcodeado a `'05'`, `CANT_DISPON` a `0`
- Cubre tanto artículos con registro en depósito (stock = 0) como sin registro (null → 0)
- `TIPO_ALERTA = 'compra'`, `ESTADO_STOCK = 'critico'`

**Reglas de negocio:**
| Tienda | Depósito 05 | Alerta generada |
|--------|-------------|-----------------|
| bajo/crítico | stock > 0 | reposicion (fila de la tienda) |
| bajo/crítico | stock = 0 o sin registro | compra (fila del depósito) |
| OK | stock = 0 o sin registro | compra (fila del depósito) |
| OK | stock > 0 | ninguna |
| Depósito (05) | — | nunca aparece en reposicion |

**Nota importante:** `PalmerAlertaService.findAll()` filtra `e != null && e.getCodArticulo() != null`
antes de mapear, para prevenir NPE por filas null que Hibernate puede generar con LEFT JOINs vacíos.

## REST API Endpoints

### Auth (pública)
| Método | Ruta |
|--------|------|
| POST | `/api/auth/login` |

### Stock — VW_PALMER_DASHBOARD
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v2/stock` | Listado paginado, 1 fila/artículo |
| GET | `/api/v2/stock/familia/{cod}` | Filtrar por familia |
| GET | `/api/v2/stock/linea/{cod}` | Filtrar por línea |
| GET | `/api/v2/stock/search?q=` | Búsqueda unificada |
| GET | `/api/v2/stock/opciones` | Familias y líneas (`@Cacheable` 6h) |

### Artículos — VW_PALMER_DETALLE + VW_PALMER_STOCK_SUCURSAL
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v2/articulos/{cod}` | Detalle del artículo |
| GET | `/api/v2/articulos/{cod}/sucursales` | Stock por sucursal |
| GET | `/api/v2/articulos/{cod}/imagen` | Imagen desde Samba (`@Cacheable` 24h) |

### Alertas — VW_PALMER_ALERTAS
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v2/alertas` | Alertas globales sin paginación (`@Cacheable` 5 min) |

Paginación: `size=20`, `sort=codArticuloNumero,ASC`. `totalPages` correcto — pagina sobre artículos únicos.
JWT requerido en todos los endpoints excepto `/api/auth/**`. La regla `/api/**` cubre `/api/v2/**`.

## Búsqueda unificada (`/api/v2/stock/search`)

Detección automática en `PalmerDashboardService`:

| Condición | Interpretación | Query |
|---|---|---|
| Solo dígitos, longitud ≤ 5 | Código de artículo | `codArticulo = q` (exacto) |
| Solo dígitos, longitud ≥ 6 | Código de barras | `codBarra = q` (exacto) |
| Contiene letras | Descripción | `articuloDes LIKE '%q%'` |
| Fallback | OR 3 campos | LIKE en los 3 campos |

Caracteres especiales (`!`, `%`, `_`) escapados por `escapeLike()` en `PalmerDashboardService`.

## EstadoStock enum

```java
// com.palmar.palmer.api.entity.EstadoStock
public enum EstadoStock { ok, bajo, critico }
```

Mapeado con `@Enumerated(EnumType.STRING)` en `PalmerStockSucursal` y `PalmerAlerta`.
El valor en Oracle (`CASE WHEN ... THEN 'critico'`) coincide exactamente con el nombre del enum.
Serializado en JSON como string en minúsculas: `"estadoStock": "critico"`.

## TipoAlerta enum

```java
// com.palmar.palmer.api.entity.TipoAlerta
public enum TipoAlerta { reposicion, compra }
```

Mapeado con `@Enumerated(EnumType.STRING)` en `PalmerAlerta`.
Serializado en JSON como string en minúsculas: `"tipoAlerta": "reposicion"`.

## DTOs V2

### PalmerDashboardDTO
| Campo | Tipo |
|-------|------|
| `codArticulo` | String |
| `articuloDes` | String |
| `codBarra` | String |
| `cantDisponTotal` | BigDecimal |
| `cantMinima` | BigDecimal |
| `nroSucursales` | Integer |
| `nroCrit` | Integer |
| `nroBajo` | Integer |
| `precioVenta` | BigDecimal |

### PalmerDetalleDTO
Todos los campos de `PalmerDashboardDTO` + `imagen`, `costoUltimo`, `precioBase`,
`descMarca`, `descListaPrecio`, `fecVigencia`, `nroSucursales`.

### PalmerStockSucursalDTO
| Campo | Tipo |
|-------|------|
| `codArticulo` / `codSucursal` | String |
| `sucursalDes` / `codEmpresa` | String |
| `cantDispon` / `cantMinima` / `cantMaxima` | BigDecimal |
| `estadoStock` | EstadoStock |

### PalmerAlertaDTO
| Campo | Tipo |
|-------|------|
| `codArticulo` / `codSucursal` | String |
| `articuloDes` / `sucursalDes` | String |
| `codBarra` | String |
| `codFamilia` / `familiaDes` | String |
| `codLinea` / `lineaDes` | String |
| `codUnidadMedida` / `descUnidadMedida` | String |
| `cantDispon` / `cantMinima` | BigDecimal |
| `estadoStock` | EstadoStock |
| `tipoAlerta` | TipoAlerta |
| `precioVenta` | BigDecimal |

## Separación de responsabilidades

### Controller → Service → Repository (regla estricta)
- Los controllers **nunca** inyectan repositorios directamente.
- `PalmerArticuloController` delega toda la lógica de datos a `PalmerArticuloService`:
  - `findDetalle()` — detalle del artículo
  - `findSucursales()` — stock por sucursal
  - `findImagenFilename()` — nombre del archivo de imagen (lanza 404 si no existe)

## Cache (Caffeine)

| Cache | TTL | Max | Propósito |
|-------|-----|-----|-----------|
| `imagenes` | 24h | 500 | Bytes de imágenes desde Samba |
| `opciones-filtro` | 6h | 1 | Familias y líneas para filtros |
| `alertas` | 5 min | 1 | Lista completa de alertas globales |

Todos los TTL configurables en `application.yaml` (`cache.*.ttl-*`).

## Persistence

- **ORM:** Spring Data JPA + Hibernate 7, `OracleDialect`
- **Pool:** HikariCP — dev: max 5, prod: max 20
- **fetch_size prod:** 500
- **DDL:** `ddl-auto: none`
- **Ordenamiento numérico:** `@Formula("TO_NUMBER(COD_ARTICULO)")` → `codArticuloNumero`
- **Paginación:** `@EnableSpringDataWebSupport(VIA_DTO)` en `PalmerApiApplication`

## Security

- JWT stateless; `/api/auth/**` pública; todo `/api/**` requiere Bearer token. Expiración: **15 minutos** (`jwt.expiration-ms: 900000`)
- Regla `requestMatchers("/api/**")` cubre tanto `/api/*` como `/api/v2/**`
- **Autenticación:** `OracleAuthenticationProvider` valida credenciales directamente contra el motor Oracle via `DriverManager.getConnection(dbUrl, props)` con timeout de 5 s (`auth.datasource.connection-timeout`). Si la conexión tiene éxito, verifica que el usuario exista en `USUARIOS` con `ESTADO='A'` y carga el rol desde `COD_GRUPO`. La conexión se abre y cierra inmediatamente (try-with-resources) — no se mantiene ningún pool por usuario.
- **AuthenticationManager:** `ProviderManager` explícito con `OracleAuthenticationProvider` como único proveedor. Solo interviene en el login — los requests JWT no pasan por aquí.
- **AppUserDetailsService:** **Código muerto** — no es invocado por ningún componente activo. `JwtAuthFilter` construye el `Authentication` directamente desde los claims del JWT (username + roles) sin consultar la BD. Candidato a eliminar o reproponer.
- `PasswordEncoder` actual: uppercase (texto plano) — ya no participa en el login. **Pendiente migrar a BCrypt.**
- CORS via `CORS_ALLOWED_ORIGINS` (todos con `https://`)

### OracleAuthenticationProvider — flujo
```
POST /api/auth/login
  → OracleAuthenticationProvider.authenticate()
  → username.toUpperCase()
  → DriverManager.getConnection(auth.datasource.url, {user, password, CONNECT_TIMEOUT=5s})
      ← ORA-01017 / ORA-28000 → BadCredentialsException → 401
      ← error inesperado → InternalAuthenticationServiceException → 500
      ← OK → misma conexión abierta, se ejecuta SELECT en USUARIOS:
              SELECT COD_USUARIO, COD_GRUPO, CLAVE_AUTORIZACION
              FROM DB_SCHEMA.USUARIOS WHERE COD_USUARIO = ? AND ESTADO = 'A'
              ← sin fila → DisabledException → 401
              ← con fila → UsuarioInfo{codUsuario, codGrupo, claveAutorizacion}
      (conexión se cierra al salir del try-with-resources)
  → UserDetails{username=COD_USUARIO, roles=[COD_GRUPO], password=CLAVE_AUTORIZACION}
  → UsernamePasswordAuthenticationToken(userDetails, null, [COD_GRUPO])
  → AuthController: jwtUtil.generateToken(userDetails) → JWT{sub, roles, exp=now+15m}
  → LoginResponseDTO{token, username, roles} → 200

Prerequisito: GRANT SELECT ON DB_SCHEMA.USUARIOS TO <cada usuario Oracle de la app>
OracleAuthenticationProvider NO depende de UsuarioRepository (eliminado del provider).

Requests autenticados (JwtAuthFilter — sin consultar BD):
  → extractValidUsername(token) → COD_USUARIO (desde claim "sub")
  → extractRoles(token) → [COD_GRUPO] (desde claim "roles")
  → UsernamePasswordAuthenticationToken(username, null, authorities) en SecurityContext
```

## Estrategia de configuración

| Tipo | Dónde |
|------|-------|
| Secretos | `.env` únicamente |
| Infraestructura | `.env` (dev) / `.env.prod` (prod) |
| Comportamiento por perfil | `application-{dev\|prod}.yaml` |
| Valores fijos | `application.yaml` base |

## Environment Variables

| Variable | Descripción |
|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` \| `prod` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales Oracle |
| `DB_URL` | `jdbc:oracle:thin:@//host:port/service` |
| `DB_SCHEMA` | Schema Oracle |
| `JWT_SECRET` | Clave HS256 |
| `CORS_ALLOWED_ORIGINS` | Orígenes separados por coma |
| `SSL_ENABLED` | `true` para HTTPS |
| `SSL_KEYSTORE_PATH` | dev: `classpath:palmer.p12` / prod: ruta absoluta |
| `SSL_KEYSTORE_PASSWORD` | Password keystore |
| `SAMBA_USER` / `SAMBA_PASSWORD` | Credenciales SMB |
| `SAMBA_HOST` / `SAMBA_SHARE` / `SAMBA_FOLDER` / `SAMBA_DOMAIN` | Conexión Samba |

## Samba / Imágenes

- `PalmerArticuloService.findImagenFilename()` resuelve el nombre desde `VW_PALMER_DETALLE.IMAGEN`
- Conexión SMB2 persistente con `ReentrantLock`. `@PostConstruct` / `@PreDestroy`.
- Caché Caffeine + HTTP (`Cache-Control: max-age=86400` + `ETag` CRC32).
- `computeEtag()` en `PalmerArticuloController` — CRC32 sobre bytes de la imagen.

## HTTPS / SSL

Servidor en **HTTPS** puerto `8282`, keystore PKCS12 generado con `mkcert`.

```powershell
mkcert -pkcs12 -p12-file palmer.p12 10.30.1.3 10.30.1.6 10.30.2.18 localhost 127.0.0.1
```

IPs: `10.30.1.3` (prod OL8), `10.30.1.6`, `10.30.2.18` (dev). Vence 1 sep 2028.

| Perfil | `SSL_KEYSTORE_PATH` | `key-store` en YAML |
|--------|---------------------|---------------------|
| `dev` | `classpath:palmer.p12` | `${SSL_KEYSTORE_PATH}` |
| `prod` | `/home/inventiva/Palmer/palmer.p12` | `file:${SSL_KEYSTORE_PATH}` |

### Producción (Oracle Linux 8 — `10.30.1.3`)
```
/home/inventiva/Palmer/
├── palmer-api-0.0.1-SNAPSHOT.jar
├── .env
├── palmer.p12
└── rootCA.pem
```

## Frontend (palmer)

- **React 19** + Vite 8, `react-router-dom` v7
- **Dependencias:** `jspdf` (generación de PDFs en cliente)
- Estructura: `src/api/`, `src/components/`, `src/hooks/`, `src/lib/`, `src/pages/`

### Páginas principales
| Página | Ruta | Descripción |
|--------|------|-------------|
| `LoginPage` | `/login` | Autenticación JWT |
| `DashboardPage` | `/` | Listado paginado de stock con filtros |
| `ArticleDetailPage` | `/article/:cod` | Detalle de artículo + stock por sucursal |
| `AlertsPage` | `/alerts` | Alertas de reposición y compra |

### AlertsPage — estructura
- **Botón 🚚 Reposición** — genera PDF con artículos agrupados por sucursal (críticos primero)
- **Botón 🛒 Compra** — genera PDF con artículos agrupados por familia + precio
- Sección **Reposición desde depósito** — `BranchCard` por sucursal con badges crítico/bajo
- Sección **Requieren compra** — card única con lista plana de artículos del depósito
- Generación PDF via `src/lib/pdf-reports.js` (`generarPDFReposicion`, `generarPDFCompra`)

### Iconos relevantes (`src/components/ui/icons.jsx`)
| Ícono | Uso |
|-------|-----|
| `IcoTruck` | Header sección Reposición + botón PDF |
| `IcoShoppingCart` | Header sección Compra + botón PDF |
| `IcoMap` | Branch cards de sucursal |
| `IcoBox` | Estado vacío |

### Design system (`src/index.css`)
Variables CSS clave para alertas:
- `--accent` / `--accent-bg` / `--accent-ring` — azul primario (reposición)
- `--crit-bg/fg/dot/ring` — rojo crítico
- `--bajo-bg/fg/dot/ring` — naranja bajo
- `--compra-bg/fg/ring` → `#fffbeb` / `#d97706` / `#fcd34d` (ámbar, compra)
- Colores por sucursal: `--suc-1` a `--suc-5`

## Pendientes

- Migrar `PasswordEncoder` de uppercase a BCrypt (requiere hashear claves en BD)
- Redirect HTTP `8281` → HTTPS `8282`: bean `servletContainer()` en `SecurityConfig.java`
- Agregar tests unitarios (actualmente 0 tests)
- Eliminar o reproponer `AppUserDetailsService` — código muerto desde que `JwtAuthFilter` construye el `Authentication` directo desde claims JWT
