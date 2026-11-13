-- Columna nueva del Kanban: "Me interesa" (interested), para guardar una oferta antes de
-- postularse. Solo se amplía la lista de estados válidos; las filas que ya existen no cambian.
ALTER TABLE job_applications DROP CONSTRAINT job_applications_status_valid;
ALTER TABLE job_applications ADD CONSTRAINT job_applications_status_valid
    CHECK (status IN ('interested', 'applied', 'in-progress', 'interview', 'rejected'));
