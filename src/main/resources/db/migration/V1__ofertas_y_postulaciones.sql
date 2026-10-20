-- Ofertas y postulaciones. Las fechas son bigint con epoch milisegundos UTC, igual que en la API,
-- así no hay conversiones de zona horaria en ninguna capa. Los textos de estado, modalidad y tipo
-- son los mismos valores que viajan en JSON.

-- Las ofertas se borran de forma lógica (deleted_at): así cada postulación sigue apuntando a una
-- oferta que existe y la llave foránea nunca se rompe.
CREATE TABLE job_offers (
    id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title                text          NOT NULL,
    company              text          NOT NULL,
    company_logo_url     text,
    location             text          NOT NULL,
    modality             text          NOT NULL,
    job_type             text          NOT NULL,
    sector               text          NOT NULL,
    description          text          NOT NULL,
    requirements         text[]        NOT NULL DEFAULT '{}',
    skills               text[]        NOT NULL DEFAULT '{}',
    min_experience_years integer,
    salary_min           numeric(12, 2),
    salary_max           numeric(12, 2),
    salary_currency      text,
    posted_at            bigint        NOT NULL,
    deleted_at           bigint,

    CONSTRAINT job_offers_modality_valid CHECK (modality IN ('remote', 'on-site', 'hybrid')),
    CONSTRAINT job_offers_job_type_valid CHECK (job_type IN ('full-time', 'part-time', 'freelance', 'internship')),
    CONSTRAINT job_offers_experience_valid CHECK (min_experience_years >= 0),
    -- El salario va completo (mínimo, máximo y moneda) o no va.
    CONSTRAINT job_offers_salary_complete CHECK (
        (salary_min IS NULL AND salary_max IS NULL AND salary_currency IS NULL)
        OR (salary_min IS NOT NULL AND salary_max IS NOT NULL AND salary_currency IS NOT NULL)),
    CONSTRAINT job_offers_salary_range CHECK (salary_min >= 0 AND salary_min <= salary_max)
);

-- El listado muestra solo las vigentes, la más nueva primero.
CREATE INDEX job_offers_posted_idx ON job_offers (posted_at DESC, id DESC) WHERE deleted_at IS NULL;

-- Cada postulación copia título, empresa, logo, ubicación y modalidad de la oferta, para que el
-- tablero se siga viendo igual si la oferta se edita o se borra.
CREATE TABLE job_applications (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          text   NOT NULL,
    job_offer_id     bigint NOT NULL REFERENCES job_offers (id),
    job_title        text   NOT NULL,
    company          text   NOT NULL,
    company_logo_url text,
    location         text   NOT NULL,
    modality         text   NOT NULL,
    status           text   NOT NULL DEFAULT 'applied',
    notes            text,
    applied_at       bigint NOT NULL,
    updated_at       bigint NOT NULL,

    CONSTRAINT job_applications_modality_valid CHECK (modality IN ('remote', 'on-site', 'hybrid')),
    CONSTRAINT job_applications_status_valid CHECK (status IN ('applied', 'in-progress', 'interview', 'rejected')),
    -- Una sola postulación por usuario y oferta. El servicio lo revisa antes de guardar, y esta
    -- restricción cubre dos solicitudes simultáneas.
    CONSTRAINT job_applications_one_per_user UNIQUE (job_offer_id, user_id)
);

-- El tablero de cada usuario, la última movida primero.
CREATE INDEX job_applications_user_idx ON job_applications (user_id, updated_at DESC, id DESC);
