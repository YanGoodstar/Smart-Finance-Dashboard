-- Agent-1 incremental schema for importjob / transaction changes.
-- import_job needs tracking fields introduced by A1-02.
-- transaction_record requires no schema change in A1-03 or A1-04.

ALTER TABLE import_job
    ADD COLUMN total_count INT NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN processed_count INT NOT NULL DEFAULT 0 AFTER total_count;
