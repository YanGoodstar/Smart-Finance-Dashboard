-- Agent-1 stage3 local SQL for import source expansion.
-- No schema change is required for WECHAT_CSV support.
-- Stage3 continues to reuse the existing import_job and transaction_record schema.

-- Review note for architect merge:
-- Keep the stage2 duplicate lookup index if it has already been applied.
