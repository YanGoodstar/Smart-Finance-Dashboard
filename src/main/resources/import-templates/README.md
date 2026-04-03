# Stage3 Import Integration Notes

## Supported Source Type

- `ALIPAY_CSV`
- `WECHAT_CSV`

## Upload API

- Path: `POST /api/import-jobs/upload`
- Content-Type: `multipart/form-data`
- Fields:
  - `file`
  - `sourceType`

## Curl Examples

```bash
curl -X POST "http://localhost:8080/api/import-jobs/upload" \
  -F "sourceType=ALIPAY_CSV" \
  -F "file=@src/main/resources/import-templates/alipay/alipay_csv_sample.csv"

curl -X POST "http://localhost:8080/api/import-jobs/upload" \
  -F "sourceType=WECHAT_CSV" \
  -F "file=@src/main/resources/import-templates/wechat/wechat_csv_sample.csv"
```

## Response Object

- `ImportJobResponse`
  - `id`
  - `sourceType`
  - `status`
  - `execution`
  - `processingSummary`
  - `createdAt`
  - `updatedAt`

## Current Import Behavior

- Upload is executed synchronously in the request lifecycle.
- Parser SPI resolves by `sourceType` and template match.
- Current parsers: `ALIPAY_CSV`, `WECHAT_CSV`.
- Both sources are normalized into the existing transaction import candidate structure.
- Classification integration continues through `RuleService#classifyTransaction(...)`.
- Duplicate detection continues to use:
  - `transactionDate + amount + direction + merchantName`
- Duplicate hits only set `suspectedDuplicate = true`; they do not reject insert.
- Parsed candidates continue to flow into:
  - `transaction_record` insert
  - `import_job` status and summary write-back

## Template Scope

- Stage3 only targets the bundled WeChat CSV integration sample.
- Stage3 does not add support for PDF, image, or Excel bill imports.
- Stage3 does not expand shared entities for WeChat-specific fields.

## Recommended Manual Verification

1. Start backend with database schema applied.
2. Upload the Alipay sample and verify the returned `ImportJobResponse`.
3. Upload the WeChat sample and verify the returned `ImportJobResponse`.
4. Query `GET /api/import-jobs/{jobId}` and confirm status, counts, and error summary were persisted.
5. Query `GET /api/transactions` and confirm imported transactions were inserted.
6. Re-upload the same sample and confirm suspected duplicate counts increase.
