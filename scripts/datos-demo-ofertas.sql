-- Ofertas de demostración para el ensayo y la defensa.
--
-- Las empresas son ficticias a propósito: son ofertas inventadas y no deben parecer de
-- empresas reales. Están pensadas para el público de la app (jóvenes con poca experiencia):
-- prácticas, medio tiempo y puestos junior, en varios sectores y provincias.
--
-- Cómo correrlo (Cloud Shell o cualquier máquina con acceso al servidor), conectado como el
-- usuario de la aplicación, que es el dueño de las tablas:
--
--   psql "host=<servidor>.postgres.database.azure.com port=5432 dbname=ofertas_postulaciones user=ofertas sslmode=require" -f datos-demo-ofertas.sql
--
-- Se puede correr varias veces: una oferta con el mismo título y empresa no se vuelve a crear.
-- Se insertan directo en la base, sin pasar por el API, así que no disparan el evento job.new.
-- Las fechas de publicación quedan escalonadas en los últimos días, relativas a cuando se corre.

INSERT INTO job_offers (title, company, location, modality, job_type, sector, description,
                        requirements, skills, min_experience_years,
                        salary_min, salary_max, salary_currency, posted_at)
SELECT d.title, d.company, d.location, d.modality, d.job_type, d.sector, d.description,
       d.requirements, d.skills, d.min_experience_years,
       d.salary_min, d.salary_max, d.salary_currency,
       (extract(epoch FROM now()) * 1000)::bigint - d.days_ago * 86400000
FROM (VALUES
    ('Desarrollador Android Junior', 'Nébula Software', 'San José', 'hybrid', 'full-time', 'Tecnología',
     'Desarrollo de funcionalidades en apps Android con Kotlin y Jetpack Compose, en un equipo con mentoría.',
     ARRAY['Bachillerato en colegio', 'Inglés intermedio'], ARRAY['Kotlin', 'Git', 'Android'], 0,
     650000, 900000, 'CRC', 1),
    ('Práctica profesional en desarrollo web', 'Pixel Verde Studio', 'Heredia', 'remote', 'internship', 'Tecnología',
     'Práctica de 6 meses construyendo sitios con React y Tailwind. Ideal para estudiantes de últimos años.',
     ARRAY['Estudiante universitario o técnico'], ARRAY['JavaScript', 'React', 'HTML', 'CSS'], 0,
     NULL, NULL, NULL, 2),
    ('Soporte técnico nivel 1', 'Conecta Servicios TI', 'Cartago', 'on-site', 'full-time', 'Tecnología',
     'Atención de incidentes por teléfono y correo, instalación de equipos y registro de casos.',
     ARRAY['Técnico medio en informática o afín'], ARRAY['Windows', 'Redes', 'Servicio al cliente'], 0,
     450000, 550000, 'CRC', 3),
    ('Asistente de análisis de datos', 'Datos Claros', 'San José', 'hybrid', 'part-time', 'Finanzas',
     'Limpieza de datos, reportes semanales en Excel y tableros en Power BI para el equipo de finanzas.',
     ARRAY['Estudiante de ingeniería, economía o afín'], ARRAY['Excel', 'SQL', 'Power BI'], 0,
     300000, 400000, 'CRC', 4),
    ('Agente bilingüe de servicio al cliente', 'Puente Global Contact', 'Alajuela', 'on-site', 'full-time', 'Servicio al cliente',
     'Atención de clientes en inglés por chat y teléfono. Capacitación pagada de tres semanas.',
     ARRAY['Inglés avanzado (B2 o más)'], ARRAY['Inglés', 'Comunicación', 'Servicio al cliente'], 0,
     550000, 650000, 'CRC', 1),
    ('Diseñador gráfico freelance', 'Taller Creativo Tico', 'Remoto', 'remote', 'freelance', 'Diseño',
     'Piezas para redes sociales y presentaciones de clientes pequeños, por proyecto.',
     ARRAY['Portafolio con al menos 5 trabajos'], ARRAY['Figma', 'Illustrator', 'Diseño gráfico'], 1,
     NULL, NULL, NULL, 6),
    ('Practicante de mercadeo digital', 'Brote Marketing', 'San José', 'hybrid', 'internship', 'Mercadeo',
     'Apoyo en campañas de redes sociales, calendario de contenido y medición de resultados.',
     ARRAY['Estudiante de mercadeo, comunicación o afín'], ARRAY['Redes sociales', 'Canva', 'Redacción'], 0,
     NULL, NULL, NULL, 5),
    ('Auxiliar contable', 'Cuentas Claras CR', 'Heredia', 'on-site', 'full-time', 'Finanzas',
     'Registro de facturas, conciliaciones bancarias y apoyo en cierres mensuales.',
     ARRAY['Técnico o estudiante de contabilidad'], ARRAY['Excel', 'Contabilidad'], 1,
     500000, 600000, 'CRC', 8),
    ('Desarrollador backend Java Jr.', 'Raíz Digital', 'Remoto', 'remote', 'full-time', 'Tecnología',
     'Mantenimiento de APIs con Spring Boot y PostgreSQL. Revisión de código diaria con el equipo.',
     ARRAY['Bachiller universitario o carrera técnica en computación'], ARRAY['Java', 'Spring Boot', 'SQL', 'Git'], 1,
     800000, 1100000, 'CRC', 2),
    ('Tutor de matemáticas medio tiempo', 'Aprende Fácil', 'Puntarenas', 'hybrid', 'part-time', 'Educación',
     'Tutorías para estudiantes de colegio por las tardes, presenciales y virtuales.',
     ARRAY['Estudiante universitario de ciencias o educación'], ARRAY['Matemáticas', 'Comunicación', 'Paciencia'], 0,
     250000, 350000, 'CRC', 9)
) AS d(title, company, location, modality, job_type, sector, description, requirements, skills,
       min_experience_years, salary_min, salary_max, salary_currency, days_ago)
WHERE NOT EXISTS (
    SELECT 1 FROM job_offers o
    WHERE o.title = d.title AND o.company = d.company AND o.deleted_at IS NULL
);
