package com.releasemanager.model;

import java.time.OffsetDateTime;

/**
 * Represents a Jira ticket extracted from a commit message.
 */
public record JiraTicket(
        String ticketId,
        String commitSha,
        String commitMessage,
        String author,
        OffsetDateTime commitDate
) {}
