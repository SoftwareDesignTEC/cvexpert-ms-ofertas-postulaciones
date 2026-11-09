-- Ofertas reales importadas de una API externa (JSearch, que junta las publicadas en LinkedIn,
-- Indeed, Glassdoor y otros sitios a través de Google for Jobs).
--
-- source       de dónde vino la oferta: 'jsearch' para las importadas, 'demo' para las inventadas
--              de scripts/datos-demo-ofertas.sql y null para las que crea un administrador.
-- external_id  id de la oferta en la fuente; con source identifica la oferta para no duplicarla
--              cuando el importador la vuelve a traer.
-- apply_url    página donde la persona se postula de verdad (el sitio de la empresa o de la bolsa).
ALTER TABLE job_offers
    ADD COLUMN source      text,
    ADD COLUMN external_id text,
    ADD COLUMN apply_url   text;

CREATE UNIQUE INDEX job_offers_external_idx ON job_offers (source, external_id)
    WHERE external_id IS NOT NULL;

-- Marca las ofertas inventadas para que el importador las retire apenas haya ofertas reales.
-- No se borran aquí: si la API todavía no está configurada, la app no se queda sin ofertas.
UPDATE job_offers
SET source = 'demo'
WHERE source IS NULL
  AND company IN ('Nébula Software', 'Pixel Verde Studio', 'Conecta Servicios TI', 'Datos Claros',
                  'Puente Global Contact', 'Taller Creativo Tico', 'Brote Marketing',
                  'Cuentas Claras CR', 'Raíz Digital', 'Aprende Fácil');
