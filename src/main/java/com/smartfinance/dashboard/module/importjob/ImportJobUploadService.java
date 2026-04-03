package com.smartfinance.dashboard.module.importjob;

import com.smartfinance.dashboard.module.importjob.dto.CreateImportJobRequest;
import com.smartfinance.dashboard.module.importjob.dto.ImportJobResponse;
import com.smartfinance.dashboard.module.importjob.dto.UpdateImportJobTrackingRequest;
import com.smartfinance.dashboard.module.importjob.entity.ImportJobStatus;
import com.smartfinance.dashboard.module.importjob.parser.BillImportParser;
import com.smartfinance.dashboard.module.importjob.parser.ImportFailure;
import com.smartfinance.dashboard.module.importjob.parser.ImportParseResult;
import com.smartfinance.dashboard.module.importjob.parser.ImportParserResolver;
import com.smartfinance.dashboard.module.importjob.parser.UploadedImportFile;
import com.smartfinance.dashboard.module.transaction.TransactionImportResult;
import com.smartfinance.dashboard.module.transaction.TransactionImportService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ImportJobUploadService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final int MAX_ERROR_SUMMARY_LENGTH = 1000;

    private final ImportJobService importJobService;
    private final ImportParserResolver importParserResolver;
    private final TransactionImportService transactionImportService;

    public ImportJobUploadService(
            ImportJobService importJobService,
            ImportParserResolver importParserResolver,
            TransactionImportService transactionImportService
    ) {
        this.importJobService = importJobService;
        this.importParserResolver = importParserResolver;
        this.transactionImportService = transactionImportService;
    }

    public ImportJobResponse upload(String sourceType, MultipartFile file) {
        String normalizedSourceType = validateSourceType(sourceType);
        UploadedImportFile uploadedFile = validateAndReadFile(normalizedSourceType, file);

        ImportJobResponse createdJob = importJobService.createImportJob(new CreateImportJobRequest(normalizedSourceType));
        Long jobId = createdJob.id();
        importJobService.updateImportJobTracking(
                jobId,
                new UpdateImportJobTrackingRequest(ImportJobStatus.PROCESSING, 0, 0, 0, 0, 0, null, null, null)
        );

        try {
            BillImportParser parser = importParserResolver.resolve(normalizedSourceType, uploadedFile);
            ImportParseResult parseResult = parser.parse(uploadedFile);
            TransactionImportResult importResult = transactionImportService.importTransactions(jobId, parseResult.candidates());

            int totalCount = parseResult.totalRows();
            int processedCount = totalCount;
            int successCount = importResult.successCount();
            int failedCount = countRowFailures(parseResult.failures()) + importResult.failedCount();
            int suspectedDuplicateCount = importResult.suspectedDuplicateCount();
            String errorSummary = buildErrorSummary(parseResult.failures(), importResult.failureMessages());

            ImportJobStatus finalStatus = resolveFinalStatus(totalCount, successCount, failedCount);
            ImportJobResponse updatedJob = importJobService.updateImportJobTracking(
                    jobId,
                    new UpdateImportJobTrackingRequest(
                            finalStatus,
                            totalCount,
                            processedCount,
                            successCount,
                            failedCount,
                            suspectedDuplicateCount,
                            errorSummary,
                            null,
                            null
                    )
            );
            return updatedJob != null ? updatedJob : importJobService.getImportJob(jobId);
        } catch (RuntimeException exception) {
            ImportJobResponse failedJob = importJobService.updateImportJobTracking(
                    jobId,
                    new UpdateImportJobTrackingRequest(
                            ImportJobStatus.FAILED,
                            0,
                            0,
                            0,
                            0,
                            0,
                            truncate(exception.getMessage(), MAX_ERROR_SUMMARY_LENGTH),
                            null,
                            null
                    )
            );
            return failedJob != null ? failedJob : importJobService.getImportJob(jobId);
        }
    }

    private String validateSourceType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            throw new IllegalArgumentException("sourceType is required");
        }
        String normalized = sourceType.trim();
        if (!ImportSourceTypes.isSupported(normalized)) {
            throw new IllegalArgumentException("Only ALIPAY_CSV and WECHAT_CSV are supported");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private UploadedImportFile validateAndReadFile(String sourceType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("file size exceeds 10MB limit");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("file name is required");
        }
        if (!originalFilename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException(sourceType + " file must use .csv extension");
        }

        try {
            return new UploadedImportFile(originalFilename.trim(), file.getContentType(), file.getBytes());
        } catch (IOException exception) {
            throw new ImportJobExecutionException("Failed to read uploaded file");
        }
    }

    private int countRowFailures(List<ImportFailure> failures) {
        int count = 0;
        for (ImportFailure failure : failures) {
            if (failure.rowNumber() != null) {
                count++;
            }
        }
        return count;
    }

    private String buildErrorSummary(List<ImportFailure> parseFailures, List<String> importFailureMessages) {
        List<String> messages = new ArrayList<>();
        for (ImportFailure failure : parseFailures) {
            if (failure.rowNumber() != null) {
                messages.add("row " + failure.rowNumber() + ": " + failure.message());
            } else {
                messages.add(failure.message());
            }
        }
        messages.addAll(importFailureMessages);
        if (messages.isEmpty()) {
            return null;
        }
        return truncate(String.join("; ", messages), MAX_ERROR_SUMMARY_LENGTH);
    }

    private ImportJobStatus resolveFinalStatus(int totalCount, int successCount, int failedCount) {
        if (totalCount == 0) {
            return ImportJobStatus.FAILED;
        }
        if (failedCount == 0) {
            return ImportJobStatus.SUCCESS;
        }
        if (successCount > 0) {
            return ImportJobStatus.PARTIAL_SUCCESS;
        }
        return ImportJobStatus.FAILED;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
