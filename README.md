# api-gateway · LogiTrack

**Puerta de entrada única** de LogiTrack (Spring Cloud Gateway), Sprint 3 de Desarrollo y Arquitecturas
Avanzadas de Software, FI UNJu 2026.

Todas las peticiones de afuera (Postman, Apidog, un frontend) entran por acá. El gateway busca el servicio
destino por su **nombre en Eureka** y le reenvía la petición tal cual, sin cambiar el path.

| | |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.0.9 · Spring Cloud 2025.1.1 (Gateway sobre WebFlux) |
| Puerto | `8066` |
| Configuración | `api-gateway.yml`, `api-gateway-dev.yml` y `api-gateway-prod.yml` en el repositorio `logitrack-config` |

## Rutas

| Path | Destino | Servicio |
|---|---|---|
| `/api/v1/auth/**` | `lb://SECURITY-SERVICE` | security-service |
| `/api/v1/repartidores/**` | `lb://COURIER-SERVICE` | courier-service |
| `/api/v1/envios/**` | `lb://SHIPMENT-SERVICE` | shipment-service |

`lb://` quiere decir que el gateway le pregunta a Eureka dónde está el servicio y reparte las peticiones
entre sus instancias. No hay ninguna IP ni puerto escrito a mano.

## Seguridad: filtro global de JWT (S3-06)

Antes de reenviar una petición, el gateway revisa el token del header `Authorization: Bearer <token>`:

- **Rutas públicas** (sin token): `/api/v1/auth/**` (login y registro) y `/actuator/health`.
- **Todo lo demás** necesita un token válido: firmado con HS256 con el mismo `JWT_SECRET` que usa
  security-service, y sin vencer.
- Si el token falta, está mal formado, vencido o con la firma alterada, el gateway responde **401** con un
  `ProblemDetail` (título `No autorizado`, mismo formato que los servicios) y la petición **no llega** al
  servicio.
- Si el token es válido, el gateway guarda el `sub` (id del usuario) y los `roles` para el filtro de roles
  y reenvía el header `Authorization` **sin cambiarlo**: los servicios siguen validando el token y
  resolviendo la autorización fina y por pertenencia.

`JWT_SECRET` llega por variable de entorno y no tiene valor por defecto: si falta o tiene menos de 32
caracteres, el gateway **no arranca**.

## Configuración

El puerto, el registro en Eureka y las rutas no están en este repositorio: los entrega el
**config-server** desde `logitrack-config`.

| Archivo | Qué tiene |
|---|---|
| `api-gateway.yml` | Puerto `8066` y las tres rutas |
| `api-gateway-dev.yml` | Eureka en `http://localhost:8065/eureka/` y el endpoint `/actuator/gateway/routes` |
| `api-gateway-prod.yml` | Eureka en `http://eureka-server:8065/eureka/` (Docker) |

| Variable | Para qué | Por defecto |
|---|---|---|
| `CONFIG_SERVER_URL` | URL del config-server | `http://localhost:8888` |
| `SPRING_PROFILES_ACTIVE` | Perfil de configuración | `dev` |
| `JWT_SECRET` | Secreto con el que se validan los JWT (el mismo de security-service, mínimo 32 caracteres) | — (obligatoria) |

## Ejecutar

Orden de arranque: `config-server` (8888) → `eureka-server` (8065) → security, courier y shipment →
este gateway.

```bash
./mvnw -B clean verify     # compila y corre los tests (no necesitan config-server ni Eureka)
./mvnw spring-boot:run
```

Desde IntelliJ, en la configuración de ejecución hay que cargar la variable de entorno
`JWT_SECRET=<la misma clave que usan los demás servicios>`.

- Health: http://localhost:8066/actuator/health
- Rutas cargadas (solo perfil `dev`): http://localhost:8066/actuator/gateway/routes
- Panel de Eureka: http://localhost:8065 (tiene que aparecer `API-GATEWAY` en estado `UP`)

Prueba rápida, con todo levantado:

```bash
curl -X POST http://localhost:8066/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin.demo","password":"admin123"}'
```

## Tests

Los tests no usan Eureka ni los servicios reales: levantan tres **servicios falsos** (un servidor HTTP
mínimo del JDK por cada servicio) y le arman al gateway las mismas rutas apuntando a ellos.

| Test | Qué comprueba |
|---|---|
| `ApiGatewayApplicationTests` | El gateway arranca y `/actuator/health` responde `UP` |
| `EnrutamientoTest` | Cada path llega a su servicio, con el mismo método, path y cuerpo; un path sin ruta da 404 |
| `JwtValidadorTest` | Secreto ausente o corto, token válido, sin roles, otra firma, otro algoritmo y texto mal formado |
| `FiltroJwtGlobalTest` | Token válido (y header reenviado sin cambios), ausente, no Bearer, mal formado, vencido, firma alterada y login sin token |
