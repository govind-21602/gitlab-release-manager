package com.releasemanager.service;

import com.releasemanager.model.CommitInfo;
import com.releasemanager.model.JiraTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JiraParserTest {

    private JiraParser parser;

    @BeforeEach
    void setUp() {
        parser = new JiraParser();
    }

    @Test
    void extractsSingleTicket() {
        List<String> ids = parser.extractIds("Fix PROJ-123: null pointer exception");
        assertEquals(List.of("PROJ-123"), ids);
    }

    @Test
    void extractsMultipleTickets() {
        List<String> ids = parser.extractIds("ABC-1 and DEF-999 implemented");
        assertEquals(2, ids.size());
        assertTrue(ids.contains("ABC-1"));
        assertTrue(ids.contains("DEF-999"));
    }

    @Test
    void deduplicatesTickets() {
        List<String> ids = parser.extractIds("PROJ-10 relates to PROJ-10");
        assertEquals(List.of("PROJ-10"), ids);
    }

    @Test
    void ignoresLowercasePatterns() {
        List<String> ids = parser.extractIds("fix proj-123 issue");
        assertTrue(ids.isEmpty());
    }

    @Test
    void returnsEmptyForBlankMessage() {
        assertTrue(parser.extractIds("   ").isEmpty());
        assertTrue(parser.extractIds(null).isEmpty());
    }

    @Test
    void parseCommitsReturnsTicketsWithMetadata() {
        CommitInfo commit = new CommitInfo(
                "abc123", "PROJ-42: add feature",
                "Alice", "alice@example.com",
                OffsetDateTime.now());
        List<JiraTicket> tickets = parser.parse(List.of(commit));
        assertEquals(1, tickets.size());
        assertEquals("PROJ-42", tickets.get(0).ticketId());
        assertEquals("abc123", tickets.get(0).commitSha());
        assertEquals("Alice", tickets.get(0).author());
    }

    @Test
    void parseReturnsEmptyForCommitsWithNoTickets() {
        CommitInfo commit = new CommitInfo(
                "def456", "chore: update readme",
                "Bob", "bob@example.com", null);
        assertTrue(parser.parse(List.of(commit)).isEmpty());
    }
}
