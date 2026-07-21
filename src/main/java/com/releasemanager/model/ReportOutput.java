package com.releasemanager.model;

/**
 * Holds the absolute file paths of the three generated report files.
 */
public record ReportOutput(
        String csvPath,
        String excelPath,
        String jsonPath
) {}
