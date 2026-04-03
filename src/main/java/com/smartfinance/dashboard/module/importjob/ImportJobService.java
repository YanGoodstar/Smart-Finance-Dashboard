package com.smartfinance.dashboard.module.importjob;

import com.smartfinance.dashboard.module.importjob.dto.CreateImportJobRequest;
import com.smartfinance.dashboard.module.importjob.dto.ImportJobPageResponse;
import com.smartfinance.dashboard.module.importjob.dto.ImportJobResponse;
import com.smartfinance.dashboard.module.importjob.dto.UpdateImportJobTrackingRequest;

public interface ImportJobService {

    ImportJobResponse createImportJob(CreateImportJobRequest request);

    ImportJobPageResponse listImportJobs(int page, int size);

    ImportJobResponse getImportJob(Long jobId);

    ImportJobResponse updateImportJobTracking(Long jobId, UpdateImportJobTrackingRequest request);
}
