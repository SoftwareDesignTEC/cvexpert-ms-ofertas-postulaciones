# CVExpert: Ofertas y Postulaciones

Microservicio de CVExpert que maneja las ofertas de trabajo, la compatibilidad de cada oferta con el perfil de la persona, las alertas de ofertas nuevas y las postulaciones con su tablero Kanban (Aplicado, En proceso, Entrevista, Rechazado).

Es uno de los cinco microservicios de la Fase II. Solo lo llama Azure API Management (APIM): ningún otro servicio ni cliente le habla directo.

- Spring Boot 4.1, Java 17, Maven.
- PostgreSQL 18 con migraciones Flyway (la misma versión en local, en las pruebas y en Azure).
- Arquitectura hexagonal: el dominio no depende de Spring, JPA ni Azure.

## Arquitectura

```
com.cvexpert.ofertas
├── domain/            Núcleo: reglas del negocio, sin dependencias de infraestructura
│   ├── model/         Entidades y valores (oferta, postulación, estados)
│   ├── exception/     Errores del negocio (no encontrado, ya postulado, sin permiso, datos inválidos)
│   └── port/
│       ├── in/        Casos de uso que ofrece el servicio
│       └── out/       Lo que el dominio necesita: guardar datos, publicar eventos
├── application/       Implementación de los casos de uso
├── adapter/
│   ├── in/web/        Controladores REST que llama APIM
│   └── out/
│       ├── persistence/   PostgreSQL con Spring Data JPA
│       └── messaging/     Azure Service Bus (eventos para Notificaciones)
└── config/            Conecta los adaptadores con los puertos
```

Cada paquete tiene un `package-info.java` que explica qué va ahí.

Cada operación del contrato es un caso de uso en `domain/port/in` (`ApplyToJobUseCase`, `AdminChangeStatusUseCase`, etc.). Los servicios de `application/` los implementan usando solo los puertos de salida, y `config/UseCaseConfig` los arma con los adaptadores. Por eso los servicios se prueban con repositorios en memoria, sin Spring ni base de datos.

## Modelo de datos

El esquema lo crea Flyway (`src/main/resources/db/migration`). Hibernate solo valida que el mapeo coincida (`ddl-auto: validate`).

| Tabla | Qué guarda |
| --- | --- |
| `job_offers` | Ofertas. `skills` y `requirements` son `text[]`. El salario va completo (mínimo, máximo y moneda) o no va |
| `job_applications` | Postulaciones, con copia del título, la empresa, el logo, la ubicación y la modalidad de la oferta |

- **Llave foránea** de `job_applications.job_offer_id` a `job_offers.id`.
- **`UNIQUE (job_offer_id, user_id)`**: una sola postulación por usuario y oferta. El servicio lo revisa antes de guardar, y la restricción cubre dos solicitudes simultáneas. El adaptador la traduce a `APPLICATION_ALREADY_EXISTS` (409).
- **`CHECK`** en estado, modalidad, tipo de jornada, experiencia y salario, con los mismos valores que viajan en JSON (`in-progress`, `on-site`, `full-time`…).
- **Borrado lógico de ofertas** (`deleted_at`): una oferta borrada deja de aparecer, pero sus postulaciones siguen apuntando a un id válido y la llave foránea nunca se rompe.
- **Fechas** en `bigint` con epoch milisegundos UTC, igual que en la API.
- Los ids son `bigint` en la base y viajan como texto en JSON.

## Identidad

APIM valida el token de Google y le pasa al servicio la identidad en encabezados:

| Encabezado | Contenido |
| --- | --- |
| `X-User-Id` | `sub` de la cuenta de Google |
| `X-User-Email` | Correo de la cuenta |
| `X-User-Role` | Solo en las rutas de administración |

El servicio no recibe ni valida tokens de Google, y rechaza las llamadas que no traen el certificado de cliente de APIM.

## Eventos

Publica en la cola `notificaciones-eventos` de Azure Service Bus, con el esquema que define Notificaciones:

| Evento | Cuándo |
| --- | --- |
| `application.status_changed` | Un administrador cambia el estado de una postulación (ruta de administración). Cuando la persona mueve su propia tarjeta o se postula, no se publica nada. |
| `job.new` | Se crea una oferta: un mensaje por cada usuario cuya compatibilidad alcanza el mínimo que fijó. |

Las fechas viajan siempre en epoch milisegundos UTC.

## Correr en local

Requisitos: JDK 17 o superior y Docker.

```bash
cp .env.example .env        # y cambiar la contraseña
docker compose up -d        # PostgreSQL 18 en localhost:5432 (o en POSTGRES_PORT)
set -a; . ./.env; set +a    # carga las variables en la terminal (Git Bash / Linux / macOS)
./mvnw spring-boot:run
```

En Windows con PowerShell, en vez de la tercera línea:

```powershell
Get-Content .env | Where-Object { $_ -match '^\w' } | ForEach-Object { $k, $v = $_ -split '=', 2; Set-Item "env:$k" $v }
```

El servicio queda en http://localhost:8080 y `GET /actuator/health` responde `{"status":"UP"}`.

## Pruebas

```bash
./mvnw test
```

- Las pruebas del dominio no usan Spring ni base de datos.
- La prueba de la aplicación levanta un PostgreSQL 18 en Docker con Testcontainers, así que Docker tiene que estar corriendo.

## CI

`.github/workflows/ci.yml` corre `./mvnw -B verify` (compila, prueba y arma el jar) en cada pull request a `main` y en cada push a `main`. El check se llama **pruebas** y es obligatorio para unir un PR. Un push nuevo a la misma rama cancela la corrida anterior. El reporte de las pruebas queda como artefacto `reporte-pruebas`.

El despliegue al App Service se suma cuando existan los recursos de Azure, y saldrá solo desde `main`.

## Despliegue en Azure

Todo está en la suscripción Azure for Students de Andrés, en el grupo de recursos `rg-ofertas-postulaciones`, región **West US**:

| Recurso | Nombre | Nivel |
| --- | --- | --- |
| Azure Database for PostgreSQL – Flexible Server | `psql-ofertas-andres` (base `ofertas_postulaciones`, usuario `ofertas`) | Burstable B1ms, 32 GB, PostgreSQL 18. Entra en los servicios gratuitos de 12 meses |
| App Service (Linux, Java 17 SE) | `app-ofertas-andres` en el plan `plan-ofertas` | Basic B1, el más barato que admite certificados de cliente |

- La app usa el usuario `ofertas`, dueño solo de su base; el administrador del servidor no se usa desde la app.
- El servidor acepta conexiones solo desde servicios de Azure y siempre con TLS (`sslmode=require`).
- El App Service tiene solo HTTPS, "siempre activo" y el certificado de cliente en modo obligatorio. App Service solo pide el certificado: la app tiene que comprobar que sea el de APIM (pendiente, ver Estado).
- `DB_URL`, `DB_USER` y `DB_PASSWORD` están en las variables de entorno del App Service, cifradas por Azure. Si el grupo centraliza los secretos en un Key Vault, se cambian por referencias `@Microsoft.KeyVault(...)` sin tocar el código.
- Para no gastar crédito al terminar el curso: borrar el grupo de recursos completo.

## Configuración

Ningún valor real está en el repositorio. Todo llega por variables de entorno: en local desde `.env` (ignorado por git) y en Azure desde el Key Vault `kv-cvexpert`.

| Variable | Para qué |
| --- | --- |
| `DB_URL` | URL JDBC de PostgreSQL |
| `DB_USER` | Usuario de la base, con permisos solo sobre `ofertas_postulaciones` |
| `DB_PASSWORD` | Su contraseña |
| `PORT` | Puerto HTTP (8080 si no se define) |

## Estado

- [x] Proyecto base, estructura hexagonal y pruebas.
- [x] Borrador del contrato OpenAPI (`docs/openapi.yaml`). Falta confirmar con la exportación de APIM las 5 operaciones marcadas `por-confirmar-apim`.
- [x] CI con pruebas en cada PR.
- [ ] Despliegue al App Service desde `main`.
- [x] Dominio, persistencia y casos de uso: modelos, los 11 casos de uso, migración V1 y adaptador JPA, con pruebas.
- [ ] Controladores REST (adaptador `in/web`).
- [ ] Compatibilidad, alertas y evento `job.new`.
- [ ] Publicación de eventos en Service Bus. Hoy `LoggingEventPublisher` es un stub que escribe `application.status_changed` en el log; falta el esquema del mensaje (Samuel) y la cola `notificaciones-eventos`.
- [ ] Certificado de cliente de APIM.
