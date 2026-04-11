package com.smartfinance.dashboard.module.importjob;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfinance.dashboard.module.importjob.dto.CreateImportJobRequest;
import com.smartfinance.dashboard.module.importjob.dto.ImportJobPageResponse;
import com.smartfinance.dashboard.module.importjob.dto.ImportJobExecutionResponse;
import com.smartfinance.dashboard.module.importjob.dto.ImportJobProcessingSummaryResponse;
import com.smartfinance.dashboard.module.importjob.dto.ImportJobResponse;
import com.smartfinance.dashboard.module.importjob.dto.UpdateImportJobTrackingRequest;
import com.smartfinance.dashboard.module.importjob.entity.ImportJob;
import com.smartfinance.dashboard.module.importjob.entity.ImportJobStatus;
import com.smartfinance.dashboard.module.importjob.mapper.ImportJobMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ImportJobServiceImpl implements ImportJobService {

    private final ImportJobMapper importJobMapper;

    public ImportJobServiceImpl(ImportJobMapper importJobMapper) {
        this.importJobMapper = importJobMapper;
    }

    @Override
    public ImportJobResponse createImportJob(CreateImportJobRequest request) {
        ImportJob importJob = new ImportJob();
        importJob.setSourceType(request.sourceType().trim());
        importJob.setStatus(ImportJobStatus.PENDING);
        importJob.setTotalCount(0);
        importJob.setProcessedCount(0);
        importJob.setSuccessCount(0);
        importJob.setFailedCount(0);
        importJob.setSuspectedDuplicateCount(0);
        importJobMapper.insert(importJob);

        ImportJob persistedImportJob = importJobMapper.selectById(importJob.getId());
        return toResponse(persistedImportJob != null ? persistedImportJob : importJob);
    }

    @Override
    public ImportJobPageResponse listImportJobs(int page, int size) {
        Page<ImportJob> pageRequest = Page.of(page + 1L, size);
        LambdaQueryWrapper<ImportJob> wrapper = new LambdaQueryWrapper<ImportJob>()
                .orderByDesc(ImportJob::getCreatedAt)
                .orderByDesc(ImportJob::getId);

        Page<ImportJob> resultPage = importJobMapper.selectPage(pageRequest, wrapper);
        List<ImportJobResponse> items = resultPage.getRecords().stream()
                .map(this::toResponse)
                .toList();

        return new ImportJobPageResponse(items, resultPage.getTotal(), page, size);
    }

    @Override
    public ImportJobResponse getImportJob(Long jobId) {
        ImportJob importJob = importJobMapper.selectById(jobId);
        return importJob == null ? null : toResponse(importJob);
    }

    @Override
    public ImportJobResponse updateImportJobTracking(Long jobId, UpdateImportJobTrackingRequest request) {
        ImportJob importJob = importJobMapper.selectById(jobId);
        if (importJob == null) {
            return null;
        }

        ImportJobStatus targetStatus = request.status() != null ? request.status() : importJob.getStatus();
        int totalCount = mergeCount(importJob.getTotalCount(), request.totalCount());
        int processedCount = mergeCount(importJob.getProcessedCount(), request.processedCount());
        int successCount = mergeCount(importJob.getSuccessCount(), request.successCount());
        int failedCount = mergeCount(importJob.getFailedCount(), request.failedCount());
        int suspectedDuplicateCount = mergeCount(
                importJob.getSuspectedDuplicateCount(),
                request.suspectedDuplicateCount()
        );

        validateCounts(totalCount, processedCount, successCount, failedCount, suspectedDuplicateCount);

        importJob.setStatus(targetStatus);
        importJob.setTotalCount(totalCount);
        importJob.setProcessedCount(processedCount);
        importJob.setSuccessCount(successCount);
        importJob.setFailedCount(failedCount);
        importJob.setSuspectedDuplicateCount(suspectedDuplicateCount);
        importJob.setErrorSummary(request.errorSummary() != null ? request.errorSummary().trim() : importJob.getErrorSummary());
        importJob.setStartedAt(resolveStartedAt(importJob, request, targetStatus));
        importJob.setFinishedAt(resolveFinishedAt(importJob, request, targetStatus));
        importJobMapper.updateById(importJob);

        ImportJob persistedImportJob = importJobMapper.selectById(jobId);
        return toResponse(persistedImportJob != null ? persistedImportJob : importJob);
    }

    private ImportJobResponse toResponse(ImportJob importJob) {
        return new ImportJobResponse(
                importJob.getId(),
                importJob.getSourceType(),
                importJob.getStatus(),
                new ImportJobExecutionResponse(
                        importJob.getStartedAt(),
                        importJob.getFinishedAt(),
                        isRunning(importJob.getStatus()),
                        isTerminal(importJob.getStatus())
                ),
                new ImportJobProcessingSummaryResponse(
                        defaultCount(importJob.getTotalCount()),
                        defaultCount(importJob.getProcessedCount()),
                        defaultCount(importJob.getSuccessCount()),
                        defaultCount(importJob.getFailedCount()),
                        defaultCount(importJob.getSuspectedDuplicateCount()),
                        importJob.getErrorSummary()
                ),
                importJob.getCreatedAt(),
                importJob.getUpdatedAt()
        );
    }

    private int mergeCount(Integer currentValue, Integer requestedValue) {
        return requestedValue == null ? defaultCount(currentValue) : requestedValue;
    }

    private int defaultCount(Integer count) {
        return count == null ? 0 : count;
    }

    private void validateCounts(
            int totalCount,
            int processedCount,
            int successCount,
            int failedCount,
            int suspectedDuplicateCount
    ) {
        if (processedCount > totalCount) {
            throw new IllegalArgumentException("processedCount must not be greater than totalCount");
        }
        if (successCount + failedCount > processedCount) {
            throw new IllegalArgumentException("successCount and failedCount must not exceed processedCount");
        }
        if (suspectedDuplicateCount > processedCount) {
            throw new IllegalArgumentException("suspectedDuplicateCount must not exceed processedCount");
        }
    }

    private LocalDateTime resolveStartedAt(
            ImportJob importJob,
            UpdateImportJobTrackingRequest request,
            ImportJobStatus targetStatus
    ) {
        if (request.startedAt() != null) {
            return request.startedAt();
        }
        if (importJob.getStartedAt() != null) {
            return importJob.getStartedAt();
        }
        return isRunning(targetStatus) || isTerminal(targetStatus) ? LocalDateTime.now() : null;
    }

    private LocalDateTime resolveFinishedAt(
            ImportJob importJob,
            UpdateImportJobTrackingRequest request,
            ImportJobStatus targetStatus
    ) {
        if (request.finishedAt() != null) {
            return request.finishedAt();
        }
        if (importJob.getFinishedAt() != null) {
            return importJob.getFinishedAt();
        }
        return isTerminal(targetStatus) ? LocalDateTime.now() : null;
    }

    private boolean isRunning(ImportJobStatus status) {
        return status == ImportJobStatus.PROCESSING;
    }

    private boolean isTerminal(ImportJobStatus status) {
        return status == ImportJobStatus.SUCCESS
                || status == ImportJobStatus.PARTIAL_SUCCESS
                || status == ImportJobStatus.FAILED;
    }
}
