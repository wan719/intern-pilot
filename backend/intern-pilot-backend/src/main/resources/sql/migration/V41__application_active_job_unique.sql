-- Apply only in a controlled release window after a fresh verified backup.
-- Read-only preflight: any returned row is a STOP condition requiring owner review.
-- SELECT user_id, job_id, COUNT(*) AS active_count
-- FROM application_record WHERE deleted = 0
-- GROUP BY user_id, job_id HAVING COUNT(*) > 1;
-- Do not delete, merge, or rewrite historical user records automatically.
-- This migration is not wired to automatic application startup.
-- Rollback candidate after disabling new writes: ALTER TABLE application_record
-- DROP INDEX uk_app_user_active_job, DROP COLUMN active_job_id;

ALTER TABLE application_record
  ADD COLUMN active_job_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN deleted = 0 THEN job_id ELSE NULL END) STORED,
  ADD UNIQUE KEY uk_app_user_active_job (user_id, active_job_id);
