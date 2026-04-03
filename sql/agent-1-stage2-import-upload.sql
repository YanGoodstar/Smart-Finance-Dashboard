-- Agent-1 stage2 local SQL for upload / parser / import execution.
-- This script does not change shared enums or final schema files.
-- Apply only after architect review.

-- Speed up suspected-duplicate lookup used during import.
CREATE INDEX idx_transaction_record_duplicate_lookup
    ON transaction_record (transaction_date, amount, direction, merchant_name);

-- Optional query helper for import job list / detail views.
CREATE INDEX idx_import_job_status_created_at
    ON import_job (status, created_at);
