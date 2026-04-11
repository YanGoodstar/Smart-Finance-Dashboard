package com.smartfinance.dashboard.module.importjob.parser;

public interface BillImportParser {

    boolean supports(String sourceType);

    boolean matchesTemplate(UploadedImportFile file);

    ImportParseResult parse(UploadedImportFile file);
}
