# CVExpert: Ofertas y Postulaciones

Microservicio de CVExpert que maneja las ofertas de trabajo, la compatibilidad de cada oferta con el perfil de la persona, las alertas de ofertas nuevas y las postulaciones con su tablero Kanban (Me interesa, Aplicado, En proceso, Entrevista, Rechazado). "Me interesa" (`interested`) se agregó en la Fase II: `POST /applications` acepta `initialStatus` para guardar la oferta directo en cualquier columna (sin él, nace en `applied`).

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
| `alert_settings` | Una fila por usuario: si quiere alertas, su mínimo de compatibilidad y su perfil de emparejamiento (habilidades, ubicación, modalidades, tipos de jornada, sectores y años de experiencia) |

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

El servicio no recibe ni valida tokens de Google, y rechaza las llamadas que no traen el certificado de cliente de APIM. App Service pide el certificado en el TLS y se lo pasa a la app en el encabezado `X-ARR-ClientCert`; `ApimCertificateFilter` compara su huella SHA-1 con `APIM_CERT_THUMBPRINT`, revisa que esté vigente y, si algo no calza, responde 403 con el código `INVALID_CLIENT_CERTIFICATE`. Solo `/actuator/health` queda fuera.

## Compatibilidad

`matchScore` (0 a 100) compara la oferta con el perfil de emparejamiento que el usuario guardó en `PUT /alerts`. Si no ha guardado perfil, `matchScore` no viene. Cada criterio da una fracción entre 0 y 1 y se pondera:

| Criterio | Peso | Cómo se mide |
| --- | --- | --- |
| Habilidades | 40 | Parte de las habilidades de la oferta que el usuario tiene, sin distinguir mayúsculas ni espacios. Si la oferta no pide ninguna, completo |
| Ubicación | 20 | Completo si la oferta es remota o si la ubicación coincide; si no, 0 |
| Disponibilidad | 20 | Completo si el tipo de jornada de la oferta está entre los que acepta el usuario, o si no indicó ninguno |
| Experiencia | 20 | Completo si cumple los años mínimos; si le faltan, proporcional (1 de 2 años da la mitad) |

Los pesos están en `application.yml` (`matching.weights`) y se pueden cambiar sin tocar código. No tienen que sumar 100: el puntaje se normaliza con la suma. La lógica está en `CompatibilityCalculator`, dentro del dominio, y `CompatibilityCalculatorTest` cubre cada criterio.

Al crear una oferta se calcula la compatibilidad con cada usuario que tiene alertas activas, y se publica un `job.new` para cada uno cuyo puntaje es mayor o igual a su `minMatch`. Editar una oferta no vuelve a avisar.

### Filtros de `GET /jobs`

Además de `search`, el listado acepta tres filtros opcionales; `totalRecords` cuenta solo las ofertas que pasan:

| Parámetro | Ejemplo | Qué hace |
| --- | --- | --- |
| `modality` | `remote,hybrid` | Solo esas modalidades (`remote`, `on-site`, `hybrid`) |
| `postedSinceDays` | `7` | Solo las publicadas en los últimos N días (1 a 365) |
| `minMatch` | `60` | Solo las que alcanzan esa compatibilidad (0 a 100). Sin perfil guardado no pasa ninguna |

`modality` y `postedSinceDays` se filtran en la consulta SQL. `minMatch` no puede: la compatibilidad se calcula en Java para cada usuario, así que con ese filtro el servicio trae las ofertas que pasan los demás, las puntúa y pagina en memoria (son decenas). Un valor inválido responde 400.

## Eventos

Publica en la cola `notificaciones-eventos` de Azure Service Bus, con el esquema que define Notificaciones:

| Evento | Cuándo |
| --- | --- |
| `application.status_changed` | Un administrador cambia el estado de una postulación (ruta de administración). Cuando la persona mueve su propia tarjeta o se postula, no se publica nada. |
| `job.new` | Se crea una oferta: un mensaje por cada usuario cuya compatibilidad alcanza el mínimo que fijó. |

Cada mensaje es un JSON con `schemaVersion` (1), `eventId` (uuid v4), `type`, `userId` (el `sub` de Google de quien recibe el aviso), `occurredAt` (epoch milisegundos UTC), `source` (`ofertas-postulaciones`) y `data`. Los ids dentro de `data` van como texto. En Service Bus, `messageId` es el `eventId`, `subject` es el `type` y `contentType` es `application/json`. Un mensaje que no cumple el esquema termina en la cola de errores de Notificaciones; `NotificationMessageTest` revisa la forma exacta.

Si el envío falla, se registra en el log y la solicitud responde igual, porque el cambio ya quedó guardado.

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

## Contrato, Swagger y Postman

- **Contrato:** `docs/openapi.yaml`. Es la única copia: el build la publica tal cual en `/openapi.yaml`.
- **Swagger UI:** `/swagger-ui.html` muestra ese mismo contrato, no uno generado del código, porque el contrato es lo que se acordó con la web y la app. En Azure, como todo menos `/actuator/health`, solo se llega a través de APIM.
- **Postman:** `postman/ofertas-postulaciones.postman_collection.json` tiene una prueba por ruta, en orden: crea una oferta, guarda alertas, se postula, mueve la tarjeta, el admin cambia el estado, retira la postulación y borra la oferta. También prueba los errores del contrato (403, 404, 409 y 400). Crea y limpia sus propios datos.

Para correr la colección en local, con el servicio levantado:

```bash
npx newman run postman/ofertas-postulaciones.postman_collection.json
```

En local los encabezados `X-User-*` los pone la colección. Para usarla a través de APIM, cambiar `baseUrl` por la URL de APIM con `/api/v1`; APIM reemplaza esos encabezados con la identidad del token de Google.

## Pruebas

```bash
./mvnw test
```

- Las pruebas del dominio no usan Spring ni base de datos.
- La prueba de la aplicación levanta un PostgreSQL 18 en Docker con Testcontainers, así que Docker tiene que estar corriendo.

## CI

`.github/workflows/ci.yml` tiene tres jobs:

| Job | Cuándo | Qué hace |
| --- | --- | --- |
| **pruebas** | Cada PR a `main` y cada push a `main` | `./mvnw -B verify`: compila, prueba y arma el jar. Es obligatorio para unir un PR. El reporte queda como artefacto `reporte-pruebas` |
| **cambios** | Push a `main` | Revisa si el push tocó `src/main/`, `pom.xml` o el wrapper de Maven. Si solo cambió documentación, no se despliega |
| **desplegar** | Push a `main` con cambios de código, o corrida manual sobre `main` (Actions → CI → Run workflow), después de **pruebas** | Despliega al App Service el mismo jar que pasó las pruebas y espera a que `/actuator/health` responda `UP` |

- En un PR, un push nuevo cancela la corrida anterior. En `main` no se cancela nada, para no cortar un despliegue a la mitad.
- GitHub entra a Azure con OIDC, sin contraseñas: la identidad administrada `id-github-ofertas` tiene una credencial federada que solo acepta tokens de `main` de este repo, y el rol **Website Contributor** solo sobre `app-ofertas-andres`.
- La organización usa el formato de *subject* con ids numéricos, así que la credencial federada espera `repo:SoftwareDesignTEC@328866100/cvexpert-ms-ofertas-postulaciones@1424842742:ref:refs/heads/main`. Con el formato sin ids (`repo:SoftwareDesignTEC/cvexpert-ms-ofertas-postulaciones:...`) Azure responde `AADSTS700213`. El subject exacto que manda GitHub aparece en el log del paso "Iniciar sesión en Azure".
- Secretos del repo: `AZURE_CLIENT_ID`, `AZURE_TENANT_ID` y `AZURE_SUBSCRIPTION_ID` (identificadores, no contraseñas). Variable: `AZURE_WEBAPP_NAME`.

## Despliegue en Azure

Todo está en la suscripción Azure for Students de Andrés, en el grupo de recursos `rg-ofertas-postulaciones`, región **West US**:

| Recurso | Nombre | Nivel |
| --- | --- | --- |
| Azure Database for PostgreSQL – Flexible Server | `psql-ofertas-andres` (base `ofertas_postulaciones`, usuario `ofertas`) | Burstable B1ms, 32 GB, PostgreSQL 18. Entra en los servicios gratuitos de 12 meses |
| App Service (Linux, Java 17 SE) | `app-ofertas-andres` en el plan `plan-ofertas` | Basic B1, el más barato que admite certificados de cliente |

- La app usa el usuario `ofertas`; el administrador del servidor (`adminofertas`) no se usa desde la app. `ofertas` tiene `USAGE` y `CREATE` sobre el esquema `public` de `ofertas_postulaciones`, y nada en otras bases. Ese permiso hace falta porque desde PostgreSQL 15 solo el dueño de la base puede crear tablas en `public`; sin él, Flyway falla al arrancar con `permission denied for schema public`. Se otorga una vez, conectado a esa base como administrador:

  ```sql
  GRANT USAGE, CREATE ON SCHEMA public TO ofertas;
  ```
- El servidor acepta conexiones solo desde servicios de Azure y siempre con TLS (`sslmode=require`).
- El App Service tiene solo HTTPS, "siempre activo" y el certificado de cliente en modo obligatorio, salvo en `/actuator/health` (la usa la prueba de humo del despliegue y solo responde `{"status":"UP"}`). App Service solo pide el certificado; que sea el de APIM lo comprueba la app (ver Identidad).
- `DB_URL`, `DB_USER` y `DB_PASSWORD` están en las variables de entorno del App Service, cifradas por Azure.
- `SERVICEBUS_CONNECTION_STRING` es una referencia `@Microsoft.KeyVault(VaultName=kv-cvexpert;SecretName=ServiceBus--SendConnectionString)`: el valor no se copia, el App Service lo lee del Key Vault del grupo con su identidad administrada.
- Para no gastar crédito al terminar el curso: borrar el grupo de recursos completo.

### Ofertas reales (JSearch)

Las ofertas que ve la app son reales. El servicio las importa de [JSearch](https://openwebninja.com/api/jsearch) (OpenWeb Ninja, por RapidAPI), una API que junta las ofertas publicadas en LinkedIn, Indeed, Computrabajo, buscojobs y otros sitios a través de Google for Jobs.

**Cómo funciona** (`config/JobImportConfig`, `application/JobImportService`, `adapter/out/jobsource/JSearchJobSource`):

1. Cada dos días a las 6 a. m. (hora de Costa Rica) y 20 s después de arrancar, el importador hace las 6 búsquedas de `importer.queries` en `application.yml` ("desarrollador junior en Costa Rica", "practicante en Costa Rica", "trabajo remoto desde Costa Rica"...). Cada una es un `GET /search-v2` con `country=cr`, `language=es`, `date_posted=month` y `num_pages=1` (hasta 10 ofertas).
2. Cada resultado se convierte a nuestro modelo: título, empresa, logo, ubicación (de `job_location`, sin "a través de LinkedIn"), modalidad (de `job_is_remote` y del texto: "remoto", "teletrabajo", "híbrido"), jornada, salario si viene completo, requisitos, fecha y `applyUrl` (la página donde se postula de verdad). El sector sale de la búsqueda, porque JSearch no lo trae.
3. Las habilidades no vienen en la API: `SkillExtractor` las busca en el título y la descripción contra un catálogo (Inglés, Excel, SQL, Java, Servicio al cliente...). Con eso funcionan la compatibilidad y los filtros.
4. Se guarda con `source = 'jsearch'` y `external_id = md5(título|empresa)`. No se usa el `job_id` de JSearch porque cambia en cada consulta; así una oferta que vuelve a aparecer se **actualiza** en vez de duplicarse.
5. A cada usuario con alertas activas se le avisa (`job.new`) de las ofertas **nuevas** de esa corrida que alcanzan su mínimo de compatibilidad: como mucho 3 por corrida (`importer.max-alerts-per-user`), las más compatibles.
6. Si una búsqueda falla (red, límite de la API), se registra y se sigue con las demás. Las 10 ofertas inventadas de la Fase I (`source = 'demo'`) se retiraron con la primera importación que trajo ofertas reales.

Además, `POST /jobs/refresh` corre el importador en ese momento (el botón "Buscar ofertas nuevas" de la app). Cualquier usuario puede pedirlo, pero entre una corrida y otra hay que esperar 10 minutos (429 `REFRESH_TOO_SOON`): cada corrida gasta 6 consultas del plan.

El log de cada corrida dice cuántas ofertas entraron: `Importación de jsearch: N nuevas, M actualizadas, F búsquedas fallidas, D de demostración retiradas, A avisos job.new`.

**Clave y cuota.** La clave está en el Key Vault (`Ofertas--JSearchApiKey` en `kv-cvexpert`) y el App Service la lee como referencia en `JSEARCH_API_KEY`. Sin clave, el importador no corre y el servicio sigue con las ofertas que tenga. El plan gratis da 200 consultas al mes: 6 búsquedas cada dos días son unas 90, y cada reinicio o despliegue gasta 6 más.

**Migraciones.** `V3__ofertas_reales.sql` agrega `source`, `external_id` y `apply_url`; `V4__ofertas_sin_duplicados.sql` retiró los duplicados que se crearon antes de usar `md5(título|empresa)`.

[`scripts/datos-demo-ofertas.sql`](scripts/datos-demo-ofertas.sql) con las 10 ofertas inventadas queda solo como referencia para correr el servicio en local sin clave de JSearch.

## Configuración

Ningún valor real está en el repositorio. Todo llega por variables de entorno: en local desde `.env` (ignorado por git) y en Azure desde las variables de entorno del App Service. Cuando exista el Key Vault del grupo, esas variables pasan a ser referencias `@Microsoft.KeyVault(...)` sin cambiar el código.

| Variable | Para qué |
| --- | --- |
| `DB_URL` | URL JDBC de PostgreSQL |
| `DB_USER` | Usuario de la base, con permisos solo sobre `ofertas_postulaciones` |
| `DB_PASSWORD` | Su contraseña |
| `PORT` | Puerto HTTP (8080 si no se define) |
| `APIM_CERT_THUMBPRINT` | Huella SHA-1 del certificado de cliente de APIM. Acepta varias separadas por coma, para cambiar de certificado sin cortar el servicio, y con o sin dos puntos. Si no está, no se valida el certificado (en local y en las pruebas) |
| `SERVICEBUS_CONNECTION_STRING` | Cadena de solo envío a la cola `notificaciones-eventos` (`ServiceBus--SendConnectionString` en `kv-cvexpert`). Si no está, los eventos solo se escriben en el log, que es lo que pasa en local y en las pruebas |
| `JSEARCH_API_KEY` | Clave de JSearch en RapidAPI (`Ofertas--JSearchApiKey` en `kv-cvexpert`, como referencia de Key Vault). Si no está, no se importan ofertas reales. `JSEARCH_BASE_URL`, `JSEARCH_KEY_HEADER` y `JSEARCH_HOST_HEADER` permiten usar OpenWeb Ninja directo en vez de RapidAPI |
| `JOBS_IMPORT_CRON` | Cuándo se importa (por defecto cada dos días a las 6 a. m.). `JOBS_IMPORT_ON_STARTUP=false` apaga la importación al arrancar |

## Estado

- [x] Proyecto base, estructura hexagonal y pruebas.
- [x] Contrato OpenAPI (`docs/openapi.yaml`), con el estado de cada operación respecto a los mocks de APIM.
- [x] Swagger UI y colección de Postman con una prueba por ruta.
- [ ] Revisar las 3 operaciones `mock-apim` con la exportación de APIM.
- [x] CI con pruebas en cada PR.
- [x] Despliegue al App Service desde `main`.
- [x] Dominio, persistencia y casos de uso: modelos, los 11 casos de uso, migración V1 y adaptador JPA, con pruebas.
- [x] Rutas de postulaciones (`/applications` y `/admin/applications/{id}/status`), con la identidad tomada de los encabezados de APIM y los errores del contrato.
- [x] Rutas de ofertas (`/jobs`) con `matchScore`, y de alertas (`/alerts`) con el perfil de emparejamiento.
- [x] Compatibilidad real con pesos configurables, y evento `job.new`.
- [x] Publicación de `application.status_changed` en Service Bus con el esquema de Notificaciones.
- [x] `SERVICEBUS_CONNECTION_STRING` en el App Service, como referencia a `kv-cvexpert` que lee la identidad del App Service (rol Key Vault Secrets User).
- [x] Validación del certificado de cliente de APIM (`ApimCertificateFilter`).
- [x] `APIM_CERT_THUMBPRINT` en el App Service, con la huella del certificado que APIM usa también con Usuarios.
- [ ] `X-User-Role` desde APIM: hasta que existan las rutas de administración en APIM, crear, editar y borrar ofertas y el cambio de estado de administración responden 403.
