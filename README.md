# CVExpert: Ofertas y Postulaciones

Microservicio de CVExpert que maneja las ofertas de trabajo, la compatibilidad de cada oferta con el perfil de la persona, las alertas de ofertas nuevas y las postulaciones con su tablero Kanban (Aplicado, En proceso, Entrevista, Rechazado).

Es uno de los cinco microservicios de la Fase II. Solo lo llama Azure API Management (APIM): ningún otro servicio ni cliente le habla directo.

- Spring Boot 4.1, Java 17, Maven.
- PostgreSQL 16 con migraciones Flyway.
- Arquitectura hexagonal: el dominio no depende de Spring, JPA ni Azure.

## Arquitectura

```
com.cvexpert.ofertas
├── domain/            Núcleo: reglas del negocio, sin dependencias de infraestructura
│   ├── model/         Entidades y valores (oferta, postulación, estados)
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
docker compose up -d        # PostgreSQL 16 en localhost:5432
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
- La prueba de la aplicación levanta un PostgreSQL 16 en Docker con Testcontainers, así que Docker tiene que estar corriendo.

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
- [ ] Pipeline de GitHub Actions.
- [ ] Dominio, persistencia y casos de uso.
- [ ] Publicación de eventos en Service Bus.
- [ ] Certificado de cliente de APIM.
