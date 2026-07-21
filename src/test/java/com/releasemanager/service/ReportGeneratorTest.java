package com.releasemanager.service;

import com.releasemanager.model.JiraTicket;
import com.releasemanager.model.MergeRequestResult;
import com.releasemanager.model.ReportOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesAllThreeReports() throws Exception {
        List<JiraTicket> tickets = List.of(
                new JiraTicket("PROJ-1", "sha1", "feat: add login",
                        "Alice", OffsetDateTime.now()),
                new JiraTicket("PROJ-2", "sha2", "fix: null pointer",
                        "Bob", OffsetDateTime.now())
        );
        MergeRequestResult mr = new MergeRequestResult(
                "https://gitlab.example.com/mr/1", 1, "Release MR",
                "develop", "staging", 2, 2,
                List.of("PROJ-1", "PROJ-2"), true
        );

        ReportGenerator generator = new ReportGenerator(tempDir.toString());
        ReportOutput output = generator.generate(tickets, mr);

        assertNotNull(output.csvPath());
        assertNotNull(output.excelPath());
        assertNotNull(output.jsonPath());
        assertTrue(Path.of(output.csvPath()).toFile().exists());
        assertTrue(Path.of(output.excelPath()).toFile().exists());
        assertTrue(Path.of(output.jsonPath()).toFile().exists());
    }

    @Test
    void generatesReportsForEmptyTicketList() throws Exception {
        MergeRequestResult mr = new MergeRequestResult(
                "https://gitlab.example.com/mr/2", 2, "Empty MR",
                "feature", "develop", 0, 0, List.of(), true
        );
        ReportGenerator generator = new ReportGenerator(tempDir.toString());
        ReportOutput output = generator.generate(List.of(), mr);

        assertTrue(Path.of(output.csvPath()).toFile().exists());
        assertTrue(Path.of(output.excelPath()).toFile().exists());
        assertTrue(Path.of(output.jsonPath()).toFile().exists());
    }
}
