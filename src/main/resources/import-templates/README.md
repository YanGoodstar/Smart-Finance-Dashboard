# Stage2 Import Integration Notes

## Supported Source Type

- `ALIPAY_CSV`

## Upload API

- Path: `POST /api/import-jobs/upload`
- Content-Type: `multipart/form-data`
- Fields:
  - `file`
  - `sourceType`

## Curl Example

```bash
curl -X POST "http://localhost:8080/api/import-jobs/upload" \
  -F "sourceType=ALIPAY_CSV" \
  -F "file=@src/main/resources/import-templates/alipay/alipay_csv_sample.csv"
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
- Current parser: `ALIPAY_CSV`.
- Suspected duplicate rule:
  - `transactionDate + amount + direction + merchantName`
- Duplicate hit only sets `suspectedDuplicate=true`; it does not reject insert.
- Classification integration is wired through `RuleService#classifyTransaction(...)`.
- If classification facade is unavailable at runtime, the import path falls back to:
  - `autoCategory = null`
  - `finalCategory = 未分类`
  - `categorySource = UNCLASSIFIED`

## Recommended Manual Verification

1. Start backend with database schema applied.
2. Run the upload curl command above.
3. Check returned `ImportJobResponse.status` and `processingSummary`.
4. Query `GET /api/import-jobs/{jobId}` to verify summary persistence.
5. Query `GET /api/transactions` to verify inserted transactions.
6. Re-upload the same sample file and confirm suspected duplicates increase.
