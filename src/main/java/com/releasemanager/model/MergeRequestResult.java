package com.releasemanager.model;

import java.util.List;

/**
 * Represents the outcome of a Merge Request creation or lookup.
 *
 * @param created {@code true} if the MR was newly created,
 *                {@code false} if an existing open MR was returned.
 */
public record MergeRequestResult(
        String url,
        int iid,
        String title,
        String sourceBranch,
        String targetBranch,
        int commitCount,
        int jiraCount,
        List<String> jiraIds,
        boolean created
) {}
