CREATE TABLE import_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    success_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    suspected_duplicate_count INT NOT NULL DEFAULT 0,
    error_summary VARCHAR(1000) NULL,
    started_at DATETIME(3) NULL,
    finished_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

CREATE TABLE transaction_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_job_id BIGINT NULL,
    transaction_date DATE NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    merchant_name VARCHAR(255) NOT NULL,
    summary VARCHAR(500) NULL,
    raw_text TEXT NULL,
    auto_category VARCHAR(64) NULL,
    final_category VARCHAR(64) NULL,
    category_source VARCHAR(32) NOT NULL,
    suspected_duplicate BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_transaction_record_import_job
        FOREIGN KEY (import_job_id) REFERENCES import_job(id)
);

CREATE INDEX idx_transaction_record_transaction_date
    ON transaction_record (transaction_date);

CREATE INDEX idx_transaction_record_final_category
    ON transaction_record (final_category);

CREATE INDEX idx_transaction_record_category_source
    ON transaction_record (category_source);
