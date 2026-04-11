package com.smartfinance.dashboard.module.importjob.parser;

public record UploadedImportFile(
        String originalFilename,
        String contentType,
        byte[] content
) {
}
