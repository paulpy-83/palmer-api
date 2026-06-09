# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Tech Stack

- **Java 21**, Spring Boot 4.0.6
- **Build tool:** Maven 3.9.15 via wrapper (`mvnw` / `mvnw.cmd`)
- **Root package:** `com.palmar.palmer.api`

## Comandos y perfiles Maven

```bash
# Dev (copia .env → target/)
.\mvnw.cmd clean package -DskipTests

# Prod (build frontend + copia .env.prod → target/.env)
.\mvnw.cmd clean package -Pprod -DskipTests

# Run
.\mvnw.cmd spring-boot:run
```

| Perfil | `.env` copiado | Frontend incluido |
|--------|----------------|-------------------|
| `dev` (default) | `.env` | No |
| `prod` | `.env.prod` → `.env` | Sí |

En prod el JAR sirve API REST + frontend estático desde `static/`.

## Arquitectura

```
src/main/java/com/palmar/palmer/api/
├── PalmerApiApplication.java   — @SpringBootApplication @EnableCaching
│                                 @EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
├── config/
│   ├── CacheConfig.java        — Caffeine (imagenes, opciones-filtro, alertas)
│   └── SambaProperties.java    — @ConfigurationProperties(prefix="samba")
├── controller/
│   ├── AuthController.java            — POST /api/auth/login, POST /api/auth/logout
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
│   └── CodigoDescDTO / OpcionesFiltroDTO / ErrorResponseDTO
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
│   └── PalmerStockSucursalMapper / PalmerAlertaMapper
├── repository/
│   ├── PalmerDashboardRepository     — findAll paginado, filtros, búsqueda, opciones
│   ├── PalmerDetalleRepository       — findByCodArticulo → Optional
│   ├── PalmerStockSucursalRepository — findByCodArticuloOrderByCodSucursalAsc → List
│   ├── PalmerAlertaRepository        — findAllOrdered → List (críticos primero)
│   └── UsuarioRepository
├── security/
│   ├── JwtUtil / JwtAuthFilter / OracleAuthenticationProvider / SecurityConfig
│   └── TokenBlacklistService  — Redis blacklist por JTI para revocación de JWT
└── service/
    ├── PalmerDashboardService   — findAll, filtros, search, getOpciones (@Cacheable)
    ├── PalmerArticuloService    — findDetalle + findSucursales + findImagenFilename
    ├── PalmerAlertaService      — findAll sin paginación (@Cacheable TTL 5 min), filtra nulls
    └── SambaService             — SMB2 persistente + Caffeine cache
```

**Regla estricta:** los controllers nunca inyectan repositorios directamente. `PalmerArticuloController` delega a `PalmerArticuloService`: `findDetalle()`, `findSucursales()`, `findImagenFilename()` (lanza 404 si no existe).

## Capa de datos

### Vistas Oracle (VW_PALMER_*)

| Vista | Cardinalidad | Propósito |
|-------|-------------|-----------|
| `VW_PALMER_DASHBOARD` | 1 fila / artículo | Listado paginado — `GROUP BY`, badges precalculados |
| `VW_PALMER_DETALLE` | 1 fila / artículo | Detalle completo — header, imagen, rentabilidad |
| `VW_PALMER_STOCK_SUCURSAL` | 1 fila / (artículo × sucursal) | Tab "Por Sucursal" — `EstadoStock` precalculado |
| `VW_PALMER_ALERTAS` | Filas de alerta reposicion + fila compra por artículo | `UNION ALL` de dos bloques |

Todas las vistas usan `COALESCE(CANT_MINIMA, 5)` — el default de stock mínimo está en Oracle, no en el frontend.

`ESTADO_STOCK` retorna `'ok'`, `'bajo'`, `'critico'` — mapean directamente al enum `EstadoStock`.

#### VW_PALMER_ALERTAS — lógica UNION ALL

**Bloque 1 — reposicion:** filas de tiendas (excluye sucursal `05`) con stock bajo/crítico, solo si el depósito `05` tiene `COALESCE(SUM(CANT_DISPON), 0) > 0` (subquery en `HAVING`).

**Bloque 2 — compra:** una fila por artículo representando al depósito `05`, solo si `COALESCE(SUM(dep.CANT_DISPON), 0) <= 0`. `COD_SUCURSAL` hardcodeado a `'05'`, `CANT_DISPON` a `0`. Sin `LEFT JOIN` a `ST_EXISTENCIA_ART`. Cubre artículos con registro (stock = 0) y sin registro (null → 0).

| Tienda | Depósito 05 | Alerta generada |
|--------|-------------|-----------------|
| bajo/crítico | stock > 0 | reposicion (fila de la tienda) |
| bajo/crítico | stock = 0 o sin registro | compra (fila del depósito) |
| OK | stock = 0 o sin registro | compra (fila del depósito) |
| OK | stock > 0 | ninguna |
| Depósito (05) | — | nunca aparece en reposicion |

`PalmerAlertaService.findAll()` filtra `e != null && e.getCodArticulo() != null` para prevenir NPE por filas null que Hibernate puede generar con LEFT JOINs vacíos.

### Persistencia

- **ORM:** Spring Data JPA + Hibernate 7, `OracleDialect`
- **Pool:** HikariCP — dev: max 5, prod: max 20
- **fetch_size prod:** 500 · **DDL:** `ddl-auto: none`
- **Ordenamiento numérico:** `@Formula("TO_NUMBER(COD_ARTICULO)")` → `codArticuloNumero`
- **Paginación:** `@EnableSpringDataWebSupport(VIA_DTO)` en `PalmerApiApplication`

### Enums de dominio

```java
public enum EstadoStock { ok, bajo, critico }  // @Enumerated(STRING)
public enum TipoAlerta  { reposicion, compra }  // @Enumerated(STRING)
```

Serializado en JSON en minúsculas: `"estadoStock": "critico"`, `"tipoAlerta": "reposicion"`. Los valores en Oracle (`CASE WHEN ... THEN 'critico'`) coinciden exactamente con los nombres del enum.

### DTOs V2

**PalmerDashboardDTO:** `codArticulo`, `articuloDes`, `codBarra`, `cantDisponTotal`, `cantMinima` `(BigDecimal)`, `nroSucursales`, `nroCrit`, `nroBajo` `(Integer)`, `precioVenta`.

**PalmerDetalleDTO:** todos los campos de `PalmerDashboardDTO` + `imagen`, `costoUltimo`, `precioBase`, `descMarca`, `descListaPrecio`, `fecVigencia`.

**PalmerStockSucursalDTO:** `codArticulo`, `codSucursal`, `sucursalDes`, `codEmpresa`, `cantDispon`, `cantMinima`, `cantMaxima`, `estadoStock`.

**PalmerAlertaDTO:** `codArticulo`, `codSucursal`, `articuloDes`, `sucursalDes`, `codBarra`, `codFamilia`, `familiaDes`, `codLinea`, `lineaDes`, `codUnidadMedida`, `descUnidadMedida`, `cantDispon`, `cantMinima`, `estadoStock`, `tipoAlerta`, `precioVenta`.

## REST API

JWT requerido en todos los endpoints excepto `/api/auth/**`. La regla `requestMatchers("/api/**")` cubre `/api/v2/**`.

### Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/auth/login` | Obtener JWT |
| POST | `/api/auth/logout` | Revocar JWT actual → 204 |
| GET | `/api/v2/stock` | Listado paginado, 1 fila/artículo |
| GET | `/api/v2/stock/familia/{cod}` | Filtrar por familia |
| GET | `/api/v2/stock/linea/{cod}` | Filtrar por línea |
| GET | `/api/v2/stock/search?q=` | Búsqueda unificada |
| GET | `/api/v2/stock/opciones` | Familias y líneas (`@Cacheable` 6h) |
| GET | `/api/v2/articulos/{cod}` | Detalle del artículo |
| GET | `/api/v2/articulos/{cod}/sucursales` | Stock por sucursal |
| GET | `/api/v2/articulos/{cod}/imagen` | Imagen desde Samba (`@Cacheable` 24h) |
| GET | `/api/v2/alertas` | Alertas globales sin paginación (`@Cacheable` 5 min) |

Paginación: `size=20`, `sort=codArticuloNumero,ASC`.

### Búsqueda unificada (`/api/v2/stock/search`)

Detección automática en `PalmerDashboardService`:

| Condición | Interpretación | Query |
|---|---|---|
| Solo dígitos, longitud ≤ 5 | Código de artículo | `codArticulo = q` (exacto) |
| Solo dígitos, longitud ≥ 6 | Código de barras | `codBarra = q` (exacto) |
| Contiene letras | Descripción | `articuloDes LIKE '%q%'` |
| Fallback | OR 3 campos | LIKE en los 3 campos |

Caracteres especiales (`!`, `%`, `_`) escapados por `escapeLike()` en `PalmerDashboardService`.

## Cache (Caffeine)

| Cache | TTL | Max | Propósito |
|-------|-----|-----|-----------|
| `imagenes` | 24h | 500 | Bytes de imágenes desde Samba |
| `opciones-filtro` | 6h | 1 | Familias y líneas para filtros |
| `alertas` | 5 min | 1 | Lista completa de alertas globales |

TTL configurables en `application.yaml` (`cache.*.ttl-*`).

## Seguridad

- JWT stateless, expiración **1 hora** (`jwt.expiration-ms: 3600000`)
- **Autenticación:** `OracleAuthenticationProvider` valida contra Oracle via `DriverManager.getConnection()` con timeout 5 s. Si la conexión tiene éxito, verifica `USUARIOS WHERE ESTADO='A'` y carga `COD_GRUPO`. La conexión se cierra inmediatamente (try-with-resources) — sin pool por usuario.
- **AuthenticationManager:** `ProviderManager` con `OracleAuthenticationProvider` como único proveedor. Solo interviene en el login.
- **JWT Blacklist:** `TokenBlacklistService` escribe el `jti` (UUID) en Redis con TTL = tiempo restante del token al logout. `JwtAuthFilter` consulta la blacklist antes de setear `SecurityContext`. Entradas expiran solas.
- `PasswordEncoder` actual: uppercase (texto plano) — **pendiente migrar a BCrypt.**
- CORS via `CORS_ALLOWED_ORIGINS` (todos con `https://`)

### Flujos de autenticación

```
LOGIN
POST /api/auth/login
  → OracleAuthenticationProvider.authenticate()
  → username.toUpperCase()
  → DriverManager.getConnection(auth.datasource.url, {user, password, CONNECT_TIMEOUT=5s})
      ← ORA-01017 / ORA-28000 → BadCredentialsException → 401
      ← error inesperado      → InternalAuthenticationServiceException → 500
      ← OK → SELECT COD_USUARIO, COD_GRUPO FROM DB_SCHEMA.USUARIOS WHERE COD_USUARIO = ? AND ESTADO = 'A'
              ← sin fila  → DisabledException → 401
              ← con fila  → UsuarioInfo{codUsuario, codGrupo}
      (conexión se cierra al salir del try-with-resources)
  → UserDetails{username=COD_USUARIO, roles=[COD_GRUPO]}
  → jwtUtil.generateToken() → JWT{jti=UUID, sub, roles, iat, exp=now+1h}
  → LoginResponseDTO{token, username, roles} → 200

Prerequisito: GRANT SELECT ON DB_SCHEMA.USUARIOS TO <cada usuario Oracle>

REQUEST AUTENTICADO
GET /api/v2/**  (Authorization: Bearer <token>)
  → JwtAuthFilter
      → extractValidUsername(token) → COD_USUARIO ✓ (firma + expiración)
      → extractJti(token) → UUID
      → tokenBlacklistService.isBlacklisted(jti)
          ← true  → no setea SecurityContext → 401
          ← false → extractRoles(token) → [COD_GRUPO]
                  → UsernamePasswordAuthenticationToken en SecurityContext
  → Controller → 200

LOGOUT
POST /api/auth/logout  (Authorization: Bearer <token>)
  → AuthController.logout()
      → extractJti(token) → UUID
      → extractRemainingTtl(token) → Duration
      → tokenBlacklistService.blacklist(jti, ttl)
      → Redis: SET "jti:blacklist:{UUID}" "" EX {segundos restantes}
  → 204 No Content
  (token inválido o expirado → JwtException silenciado → 204 igualmente)
```

### JWT Blacklist (Redis)

| Propiedad | Valor |
|-----------|-------|
| Implementación | `TokenBlacklistService` — `StringRedisTemplate` |
| Clave Redis | `jti:blacklist:{UUID}` |
| TTL | Tiempo restante del JWT al momento del logout |
| Redis no disponible | App falla al iniciar (dependencia requerida) |
| Crecimiento | Acotado — las entradas expiran con el JWT |

`spring.data.redis.repositories.enabled: false` desactiva el auto-scan de repositorios Redis (sin esto Spring Data Redis emite warnings por cada repositorio JPA).

### Logging de seguridad

| Prefijo | Clase | Qué registra |
|---------|-------|--------------|
| `[AUTH-LOGIN]` | `AuthController` | Intento de login, token generado con `jti` y roles |
| `[AUTH-LOGOUT]` | `AuthController` | Header recibido, `user`, `jti`, TTL, decisión de blacklistear |
| `[BLACKLIST]` | `TokenBlacklistService` | Clave Redis + TTL en cada `SET`; resultado en cada `CHECK` |
| `[JWT-FILTER]` | `JwtAuthFilter` | Cada request: método+URI, username, `jti`, resultado blacklist |

Activos con `com.palmar: DEBUG` en `application-dev.yaml`.

## Configuración

### Estrategia

| Tipo | Dónde |
|------|-------|
| Secretos | `.env` únicamente |
| Infraestructura | `.env` (dev) / `.env.prod` (prod) |
| Comportamiento por perfil | `application-{dev\|prod}.yaml` |
| Valores fijos | `application.yaml` base |

### Variables de entorno

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
| `REDIS_HOST` | Host Redis (default: `localhost`) |
| `REDIS_PORT` | Puerto Redis (default: `6379`) |
| `REDIS_PASSWORD` | Password Redis (vacío si sin auth) |
| `REDIS_DB` | Base de datos Redis (default: `0`) |

## Infraestructura

### Samba / Imágenes

- `PalmerArticuloService.findImagenFilename()` resuelve el nombre desde `VW_PALMER_DETALLE.IMAGEN`
- Conexión SMB2 persistente con `ReentrantLock`. `@PostConstruct` / `@PreDestroy`.
- Caché Caffeine + HTTP (`Cache-Control: max-age=86400` + `ETag` CRC32).
- `computeEtag()` en `PalmerArticuloController` — CRC32 sobre bytes de la imagen.

### HTTPS / SSL

Servidor en **HTTPS** puerto `8282`, keystore PKCS12 con `mkcert`:

```powershell
mkcert -pkcs12 -p12-file palmer.p12 10.30.1.3 10.30.1.6 10.30.2.18 localhost 127.0.0.1
```

IPs: `10.30.1.3` (prod OL8), `10.30.1.6`, `10.30.2.18` (dev). Vence 1 sep 2028.

| Perfil | `SSL_KEYSTORE_PATH` | `key-store` en YAML |
|--------|---------------------|---------------------|
| `dev` | `classpath:palmer.p12` | `${SSL_KEYSTORE_PATH}` |
| `prod` | `/home/inventiva/Palmer/palmer.p12` | `file:${SSL_KEYSTORE_PATH}` |

Layout en producción (Oracle Linux 8 — `10.30.1.3`):
```
/home/inventiva/Palmer/
├── palmer-api-0.0.1-SNAPSHOT.jar
├── .env
├── palmer.p12
└── rootCA.pem
```

## Frontend (palmer)

- **React 19** + Vite 8, `react-router-dom` v7, `jspdf` (PDFs en cliente)
- Estructura: `src/api/`, `src/components/`, `src/hooks/`, `src/lib/`, `src/pages/`

### Páginas

| Página | Ruta | Descripción |
|--------|------|-------------|
| `LoginPage` | `/login` | Autenticación JWT |
| `DashboardPage` | `/` | Listado paginado de stock con filtros |
| `ArticleDetailPage` | `/article/:cod` | Detalle de artículo + stock por sucursal |
| `AlertsPage` | `/alerts` | Alertas de reposición y compra |

### AlertsPage

- **Botón Reposición** — PDF con artículos agrupados por sucursal (críticos primero)
- **Botón Compra** — PDF con artículos agrupados por familia + precio
- Sección **Reposición desde depósito** — `BranchCard` por sucursal con badges crítico/bajo
- Sección **Requieren compra** — card única con lista plana de artículos del depósito
- PDFs via `src/lib/pdf-reports.js` (`generarPDFReposicion`, `generarPDFCompra`)

### AuthContext — pitfall del logout

`AuthContext.logout(expired = false)` **nunca debe pasarse directamente como `onClick`**:

```jsx
// MAL — React pasa el SyntheticEvent como `expired` (truthy) → apiLogout nunca se llama
<button onClick={logout}>

// BIEN
<button onClick={() => logout()}>
```

Si `expired` recibe un `SyntheticEvent`, el guard `if (!expired && tokenRef.current)` falla y el token no se revoca en el backend. El usuario es redirigido a `/login` igualmente, pero la sesión sigue activa en Redis.

### Design system (`src/index.css`)

Variables CSS clave para alertas:
- `--accent` / `--accent-bg` / `--accent-ring` — azul primario (reposición)
- `--crit-bg/fg/dot/ring` — rojo crítico · `--bajo-bg/fg/dot/ring` — naranja bajo
- `--compra-bg/fg/ring` → `#fffbeb` / `#d97706` / `#fcd34d` (ámbar, compra)
- Colores por sucursal: `--suc-1` a `--suc-5`

Iconos (`src/components/ui/icons.jsx`): `IcoTruck` (reposición), `IcoShoppingCart` (compra), `IcoMap` (sucursal), `IcoBox` (vacío).

## Pendientes

- Migrar `PasswordEncoder` de uppercase a BCrypt (requiere hashear claves en BD)
- Redirect HTTP `8281` → HTTPS `8282`: bean `servletContainer()` en `SecurityConfig.java`
- Agregar tests unitarios (actualmente 0 tests)
- Implementar refresh token para extender sesiones sin re-login
