package com.smartfinance.dashboard.module.importjob.parser;

import com.smartfinance.dashboard.module.importjob.ImportJobExecutionException;
import com.smartfinance.dashboard.module.importjob.ImportSourceTypes;
import com.smartfinance.dashboard.module.transaction.dto.NormalizedTransactionCandidate;
import com.smartfinance.dashboard.module.transaction.enums.TransactionDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class AlipayCsvBillImportParser implements BillImportParser {

    private static final List<Charset> CHARSET_CANDIDATES = List.of(
            StandardCharsets.UTF_8,
            Charset.forName("GB18030"),
            Charset.forName("GBK")
    );

    private static final List<String> DATE_HEADERS = List.of(
            "\u4EA4\u6613\u521B\u5EFA\u65F6\u95F4",
            "\u4EA4\u6613\u65F6\u95F4",
            "\u4ED8\u6B3E\u65F6\u95F4"
    );
    private static final List<String> AMOUNT_HEADERS = List.of(
            "\u91D1\u989D(\u5143)",
            "\u91D1\u989D",
            "\u8BA2\u5355\u91D1\u989D(\u5143)",
            "\u8BA2\u5355\u91D1\u989D"
    );
    private static final List<String> DIRECTION_HEADERS = List.of(
            "\u6536/\u652F",
            "\u8D44\u91D1\u65B9\u5411",
            "\u6536\u652F\u7C7B\u578B"
    );
    private static final List<String> MERCHANT_HEADERS = List.of(
            "\u4EA4\u6613\u5BF9\u65B9",
            "\u5BF9\u65B9",
            "\u4EA4\u6613\u5BF9\u624B"
    );
    private static final List<String> SUMMARY_HEADERS = List.of(
            "\u5546\u54C1\u540D\u79F0",
            "\u5546\u54C1\u8BF4\u660E",
            "\u5546\u54C1"
    );
    private static final List<String> REMARK_HEADERS = List.of(
            "\u5907\u6CE8",
            "\u8BF4\u660E"
    );
    private static final List<String> DATE_PATTERNS = List.of(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd"
    );

    @Override
    public boolean supports(String sourceType) {
        return ImportSourceTypes.ALIPAY_CSV.equalsIgnoreCase(sourceType);
    }

    @Override
    public boolean matchesTemplate(UploadedImportFile file) {
        return analyzeFile(file) != null;
    }

    @Override
    public ImportParseResult parse(UploadedImportFile file) {
        ParsedCsvFile parsedCsvFile = analyzeFile(file);
        if (parsedCsvFile == null) {
            throw new ImportJobExecutionException("Failed to recognize ALIPAY_CSV template");
        }

        List<NormalizedTransactionCandidate> candidates = new ArrayList<>();
        List<ImportFailure> failures = new ArrayList<>();
        int totalRows = 0;

        for (int lineIndex = parsedCsvFile.headerLineIndex() + 1; lineIndex < parsedCsvFile.lines().size(); lineIndex++) {
            List<String> cells = parseCsvLine(parsedCsvFile.lines().get(lineIndex));
            if (isIgnorableRow(cells, parsedCsvFile.dateIndex())) {
                continue;
            }

            totalRows++;
            try {
                candidates.add(toCandidate(lineIndex + 1, cells, parsedCsvFile));
            } catch (IllegalArgumentException exception) {
                failures.add(new ImportFailure(lineIndex + 1, exception.getMessage()));
            }
        }

        if (totalRows == 0) {
            failures.add(new ImportFailure(null, "No transaction rows found in uploaded file"));
        }

        return new ImportParseResult(totalRows, List.copyOf(candidates), List.copyOf(failures));
    }

    private ParsedCsvFile analyzeFile(UploadedImportFile file) {
        for (Charset charset : CHARSET_CANDIDATES) {
            ParsedCsvFile parsedCsvFile = analyzeLines(file.content(), charset);
            if (parsedCsvFile != null) {
                return parsedCsvFile;
            }
        }
        return null;
    }

    private ParsedCsvFile analyzeLines(byte[] content, Charset charset) {
        String decoded = new String(content, charset).replace("\uFEFF", "");
        List<String> lines = decoded.lines().toList();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            List<String> cells = parseCsvLine(lines.get(lineIndex));
            int dateIndex = findHeaderIndex(cells, DATE_HEADERS);
            int amountIndex = findHeaderIndex(cells, AMOUNT_HEADERS);
            int directionIndex = findHeaderIndex(cells, DIRECTION_HEADERS);
            int merchantIndex = findHeaderIndex(cells, MERCHANT_HEADERS);
            if (dateIndex < 0 || amountIndex < 0 || directionIndex < 0 || merchantIndex < 0) {
                continue;
            }

            return new ParsedCsvFile(
                    lines,
                    cells,
                    lineIndex,
                    dateIndex,
                    amountIndex,
                    directionIndex,
                    merchantIndex,
                    findHeaderIndex(cells, SUMMARY_HEADERS),
                    findHeaderIndex(cells, REMARK_HEADERS)
            );
        }
        return null;
    }

    private NormalizedTransactionCandidate toCandidate(int rowNumber, List<String> cells, ParsedCsvFile parsedCsvFile) {
        String merchantName = readOptionalCell(cells, parsedCsvFile.merchantIndex());
        String summary = combineSummary(
                readOptionalCell(cells, parsedCsvFile.summaryIndex()),
                readOptionalCell(cells, parsedCsvFile.remarkIndex())
        );
        if (merchantName == null || merchantName.isBlank()) {
            merchantName = summary;
        }
        if (merchantName == null || merchantName.isBlank()) {
            throw new IllegalArgumentException("merchantName is required");
        }

        return new NormalizedTransactionCandidate(
                rowNumber,
                ImportSourceTypes.ALIPAY_CSV,
                parseTransactionDate(readCell(cells, parsedCsvFile.dateIndex())),
                parseAmount(readCell(cells, parsedCsvFile.amountIndex())),
                parseDirection(readCell(cells, parsedCsvFile.directionIndex())),
                merchantName.trim(),
                summary,
                buildRawText(parsedCsvFile.headers(), cells)
        );
    }

    private boolean isIgnorableRow(List<String> cells, int dateIndex) {
        if (cells.stream().allMatch(cell -> cell == null || cell.trim().isEmpty())) {
            return true;
        }
        String dateCell = readOptionalCell(cells, dateIndex);
        if (dateCell != null && looksLikeDateValue(dateCell)) {
            return false;
        }
        String joined = String.join(" ", cells).trim();
        return joined.contains("\u603B\u8BA1")
                || joined.contains("\u5408\u8BA1")
                || joined.contains("\u7EDF\u8BA1")
                || joined.contains("\u8BF4\u660E")
                || joined.contains("\u4E0B\u8F7D\u8D77\u59CB\u65F6\u95F4")
                || joined.contains("\u4E0B\u8F7D\u7EC8\u6B62\u65F6\u95F4");
    }

    private LocalDate parseTransactionDate(String value) {
        String normalized = value.trim();
        for (String pattern : DATE_PATTERNS) {
            try {
                if (pattern.contains("HH")) {
                    return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern(pattern, Locale.CHINA))
                            .toLocalDate();
                }
                return LocalDate.parse(normalized, DateTimeFormatter.ofPattern(pattern, Locale.CHINA));
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("transactionDate is invalid");
    }

    private BigDecimal parseAmount(String value) {
        String normalized = value.trim()
                .replace("\uFFE5", "")
                .replace("\u00A5", "")
                .replace(",", "");
        if (normalized.startsWith("+") || normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("amount is invalid");
        }
    }

    private TransactionDirection parseDirection(String value) {
        String normalized = value.trim();
        if (normalized.contains("\u652F\u51FA") || normalized.contains("\u4ED8\u6B3E")) {
            return TransactionDirection.EXPENSE;
        }
        if (normalized.contains("\u6536\u5165") || normalized.contains("\u6536\u6B3E")) {
            return TransactionDirection.INCOME;
        }
        throw new IllegalArgumentException("direction is invalid");
    }

    private String combineSummary(String primary, String remark) {
        boolean hasPrimary = primary != null && !primary.isBlank();
        boolean hasRemark = remark != null && !remark.isBlank();
        if (hasPrimary && hasRemark) {
            if (primary.trim().equals(remark.trim())) {
                return primary.trim();
            }
            return primary.trim() + " " + remark.trim();
        }
        if (hasPrimary) {
            return primary.trim();
        }
        if (hasRemark) {
            return remark.trim();
        }
        return null;
    }

    private String buildRawText(List<String> headers, List<String> cells) {
        List<String> pairs = new ArrayList<>();
        int size = Math.min(headers.size(), cells.size());
        for (int index = 0; index < size; index++) {
            String header = normalizeHeader(headers.get(index));
            if (header.isBlank()) {
                continue;
            }
            pairs.add(header + "=" + readOptionalCell(cells, index));
        }
        return String.join(" | ", pairs);
    }

    private int findHeaderIndex(List<String> headers, List<String> aliases) {
        for (int index = 0; index < headers.size(); index++) {
            String normalizedHeader = normalizeHeader(headers.get(index));
            for (String alias : aliases) {
                if (normalizedHeader.equals(normalizeHeader(alias))) {
                    return index;
                }
            }
        }
        return -1;
    }

    private boolean looksLikeDateValue(String value) {
        String trimmed = value.trim();
        return trimmed.matches("\\d{4}[-/]\\d{2}[-/]\\d{2}.*");
    }

    private String normalizeHeader(String value) {
        return value == null
                ? ""
                : value.trim()
                .replace("\uFEFF", "")
                .replace(" ", "")
                .replace("\t", "")
                .replace("\uFF08", "(")
                .replace("\uFF09", ")");
    }

    private String readCell(List<String> cells, int index) {
        String value = readOptionalCell(cells, index);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("required column is blank");
        }
        return value;
    }

    private String readOptionalCell(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return null;
        }
        String value = cells.get(index);
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (currentChar == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (currentChar == ',' && !inQuotes) {
                cells.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(currentChar);
        }

        cells.add(current.toString());
        return cells;
    }

    private record ParsedCsvFile(
            List<String> lines,
            List<String> headers,
            int headerLineIndex,
            int dateIndex,
            int amountIndex,
            int directionIndex,
            int merchantIndex,
            int summaryIndex,
            int remarkIndex
    ) {
    }
}
