package com.smartfinance.dashboard.module.importjob;

import com.smartfinance.dashboard.common.api.ApiResponse;
import com.smartfinance.dashboard.common.api.ErrorCode;
import com.smartfinance.dashboard.module.importjob.dto.CreateImportJobRequest;
import com.smartfinance.dashboard.module.importjob.dto.ImportJobPageResponse;
import com.smartfinance.dashboard.module.importjob.dto.ImportJobResponse;
import com.smartfinance.dashboard.module.importjob.dto.UpdateImportJobTrackingRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/import-jobs")
public class ImportJobController {

    private final ImportJobService importJobService;

    public ImportJobController(ImportJobService importJobService) {
        this.importJobService = importJobService;
    }

    @PostMapping
    public ApiResponse<ImportJobResponse> createImportJob(@Valid @RequestBody CreateImportJobRequest request) {
        return ApiResponse.success(importJobService.createImportJob(request));
    }

    @GetMapping
    public ApiResponse<ImportJobPageResponse> listImportJobs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(importJobService.listImportJobs(page, size));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<ImportJobResponse>> getImportJob(@PathVariable Long jobId) {
        ImportJobResponse response = importJobService.getImportJob(jobId);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR, "Import job not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{jobId}/tracking")
    public ResponseEntity<ApiResponse<ImportJobResponse>> updateImportJobTracking(
            @PathVariable Long jobId,
            @Valid @RequestBody UpdateImportJobTrackingRequest request
    ) {
        ImportJobResponse response;
        try {
            response = importJobService.updateImportJobTracking(jobId, request);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR, exception.getMessage()));
        }
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR, "Import job not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
