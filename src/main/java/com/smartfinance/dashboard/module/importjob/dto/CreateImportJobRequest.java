package com.smartfinance.dashboard.module.importjob.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateImportJobRequest(
        @NotBlank @Size(max = 64) String sourceType
) {
}
