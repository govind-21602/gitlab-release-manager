package com.releasemanager.model;

import java.time.OffsetDateTime;

/**
 * Represents a single Git commit retrieved from the GitLab API.
 */
public record CommitInfo(
        String sha,
        String message,
        String authorName,
        String authorEmail,
        OffsetDateTime committedDate
) {}
