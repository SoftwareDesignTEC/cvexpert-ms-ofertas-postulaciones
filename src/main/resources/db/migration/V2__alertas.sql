-- Configuración de alertas y perfil de emparejamiento: una fila por usuario. El perfil sale del
-- servicio de Usuarios, pero lo manda el cliente y se guarda aquí para calcular la compatibilidad
-- de cada oferta sin llamar a otro servicio.
CREATE TABLE alert_settings (
    user_id          text    PRIMARY KEY,
    enabled          boolean NOT NULL,
    min_match        integer NOT NULL,
    skills           text[]  NOT NULL DEFAULT '{}',
    location         text    NOT NULL,
    modalities       text[]  NOT NULL DEFAULT '{}',
    job_types        text[]  NOT NULL DEFAULT '{}',
    sectors          text[]  NOT NULL DEFAULT '{}',
    experience_years integer NOT NULL,
    updated_at       bigint  NOT NULL,

    CONSTRAINT alert_settings_min_match_valid CHECK (min_match BETWEEN 0 AND 100),
    CONSTRAINT alert_settings_experience_valid CHECK (experience_years >= 0),
    -- Los arreglos solo admiten los valores del contrato. <@ es "está contenido en".
    CONSTRAINT alert_settings_modalities_valid CHECK (modalities <@ ARRAY['remote', 'on-site', 'hybrid']),
    CONSTRAINT alert_settings_job_types_valid
        CHECK (job_types <@ ARRAY['full-time', 'part-time', 'freelance', 'internship'])
);

-- Al publicar una oferta se recorren solo las configuraciones con alertas activas.
CREATE INDEX alert_settings_enabled_idx ON alert_settings (user_id) WHERE enabled;
