-- Las ofertas de JSearch se estaban duplicando: en la v5 de la API el job_id de una misma oferta
-- cambia en cada consulta, así que cada importación la volvía a crear. Desde ahora el importador
-- identifica cada oferta por md5(lower(título) || '|' || lower(empresa)) (ver
-- JSearchJobSource.stableId). Esta migración deja la base igual que si siempre hubiera sido así.

-- 1. Retira los duplicados: de cada título + empresa queda la oferta más vieja (el id menor), que
--    es a la que apuntan las postulaciones hechas antes. Los retirados conservan su external_id
--    viejo, que no choca con los nuevos.
UPDATE job_offers o
SET deleted_at = (extract(epoch FROM now()) * 1000)::bigint
FROM (
    SELECT id,
           row_number() OVER (PARTITION BY lower(title), lower(company) ORDER BY id) AS rn
    FROM job_offers
    WHERE source = 'jsearch' AND deleted_at IS NULL
) d
WHERE o.id = d.id AND d.rn > 1;

-- 2. Las que quedan pasan al identificador nuevo, para que la próxima importación las actualice
--    en vez de crearlas otra vez.
UPDATE job_offers
SET external_id = md5(lower(title) || '|' || lower(company))
WHERE source = 'jsearch' AND deleted_at IS NULL;
