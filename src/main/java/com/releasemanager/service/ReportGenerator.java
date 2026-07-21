package com.releasemanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.CSVWriter;
import com.releasemanager.model.JiraTicket;
import com.releasemanager.model.MergeRequestResult;
import com.releasemanager.model.ReportOutput;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates CSV, Excel (.xlsx) and JSON reports from the list of Jira tickets
 * and the Merge Request result.
 */
public class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String[] CSV_HEADER =
            {"Jira Ticket", "Commit SHA", "Author", "Commit Date", "Commit Message"};

    private final String outputDir;
    private final ObjectMapper objectMapper;

    public ReportGenerator(String outputDir) {
        this.outputDir = outputDir;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Generates all three report formats and returns the file paths.
     *
     * @param tickets list of Jira tickets
     * @param mr      merge request result
     * @return {@link ReportOutput} with absolute file paths
     * @throws IOException on file I/O errors
     */
    public ReportOutput generate(List<JiraTicket> tickets, MergeRequestResult mr)
            throws IOException {

        String timestamp = LocalDateTime.now().format(TS);
        Path dir = Path.of(outputDir);
        Files.createDirectories(dir);

        String csvPath   = writeCsv(tickets, dir, timestamp);
        String excelPath = writeExcel(tickets, mr, dir, timestamp);
        String jsonPath  = writeJson(tickets, mr, dir, timestamp);

        log.info("Reports written to: {}", dir.toAbsolutePath());
        return new ReportOutput(csvPath, excelPath, jsonPath);
    }

    // ── CSV ────────────────────────────────────────────────────────────────

    private String writeCsv(List<JiraTicket> tickets, Path dir, String ts)
            throws IOException {
        Path file = dir.resolve("report_" + ts + ".csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(file.toFile()))) {
            writer.writeNext(CSV_HEADER);
            for (JiraTicket t : tickets) {
                writer.writeNext(new String[]{
                        t.ticketId(),
                        t.commitSha(),
                        t.author(),
                        t.commitDate() != null ? t.commitDate().toString() : "",
                        t.commitMessage().replace("\n", " ")
                });
            }
        }
        log.info("CSV report: {}", file);
        return file.toAbsolutePath().toString();
    }

    // ── Excel ──────────────────────────────────────────────────────────────

    private String writeExcel(List<JiraTicket> tickets, MergeRequestResult mr,
                               Path dir, String ts) throws IOException {
        Path file = dir.resolve("report_" + ts + ".xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Jira Tickets");

            // Header row
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row header = sheet.createRow(0);
            for (int i = 0; i < CSV_HEADER.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(CSV_HEADER[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (JiraTicket t : tickets) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(t.ticketId());
                row.createCell(1).setCellValue(t.commitSha());
                row.createCell(2).setCellValue(t.author());
                row.createCell(3).setCellValue(
                        t.commitDate() != null ? t.commitDate().toString() : "");
                row.createCell(4).setCellValue(
                        t.commitMessage().replace("\n", " "));
            }

            // Summary sheet
            Sheet summary = wb.createSheet("Summary");
            addSummaryRow(summary, 0, "MR URL",      mr.url());
            addSummaryRow(summary, 1, "MR IID",      "!" + mr.iid());
            addSummaryRow(summary, 2, "Source",      mr.sourceBranch());
            addSummaryRow(summary, 3, "Target",      mr.targetBranch());
            addSummaryRow(summary, 4, "Commits",     String.valueOf(mr.commitCount()));
            addSummaryRow(summary, 5, "Jira Tickets", String.valueOf(mr.jiraCount()));

            for (int i = 0; i < CSV_HEADER.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
                wb.write(fos);
            }
        }
        log.info("Excel report: {}", file);
        return file.toAbsolutePath().toString();
    }

    private void addSummaryRow(Sheet sheet, int rowIdx, String key, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value);
    }

    // ── JSON ───────────────────────────────────────────────────────────────

    private String writeJson(List<JiraTicket> tickets, MergeRequestResult mr,
                              Path dir, String ts) throws IOException {
        Path file = dir.resolve("report_" + ts + ".json");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mergeRequest", mr);
        payload.put("tickets", tickets);
        objectMapper.writeValue(file.toFile(), payload);
        log.info("JSON report: {}", file);
        return file.toAbsolutePath().toString();
    }
}
