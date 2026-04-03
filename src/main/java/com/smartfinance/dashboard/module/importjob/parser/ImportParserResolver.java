package com.smartfinance.dashboard.module.importjob.parser;

import com.smartfinance.dashboard.module.importjob.ImportJobExecutionException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImportParserResolver {

    private final List<BillImportParser> parsers;

    public ImportParserResolver(List<BillImportParser> parsers) {
        this.parsers = parsers;
    }

    public BillImportParser resolve(String sourceType, UploadedImportFile file) {
        return parsers.stream()
                .filter(parser -> parser.supports(sourceType))
                .filter(parser -> parser.matchesTemplate(file))
                .findFirst()
                .orElseThrow(() -> new ImportJobExecutionException(
                        "Uploaded file does not match the requested sourceType template"
                ));
    }
}
