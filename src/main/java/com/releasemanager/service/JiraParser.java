package com.releasemanager.service;

import com.releasemanager.model.CommitInfo;
import com.releasemanager.model.JiraTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts Jira ticket IDs from commit messages.
 *
 * <p>Matches patterns such as {@code PROJ-123}, {@code ABC-1}, etc.
 */
public class JiraParser {

    private static final Logger log = LoggerFactory.getLogger(JiraParser.class);

    /**
     * Pattern that matches Jira-style ticket IDs: one or more uppercase letters,
     * a hyphen, then one or more digits.
     */
    private static final Pattern JIRA_PATTERN =
            Pattern.compile("\\b([A-Z][A-Z0-9]+-\\d+)\\b");

    /**
     * Parses all commits and returns a flat list of {@link JiraTicket} objects.
     * A single commit may produce multiple tickets if the message references more
     * than one Jira ID.
     *
     * @param commits list of commits to parse
     * @return list of extracted Jira tickets (may be empty, never {@code null})
     */
    public List<JiraTicket> parse(List<CommitInfo> commits) {
        List<JiraTicket> tickets = new ArrayList<>();

        for (CommitInfo commit : commits) {
            List<String> ids = extractIds(commit.message());
            if (ids.isEmpty()) {
                log.debug("No Jira ticket found in commit {}: {}",
                        commit.sha().substring(0, Math.min(8, commit.sha().length())),
                        commit.message());
            }
            for (String id : ids) {
                tickets.add(new JiraTicket(
                        id,
                        commit.sha(),
                        commit.message(),
                        commit.authorName(),
                        commit.committedDate()
                ));
                log.debug("Found Jira ticket {} in commit {}", id,
                        commit.sha().substring(0, Math.min(8, commit.sha().length())));
            }
        }

        log.info("Extracted {} Jira ticket reference(s) from {} commit(s)",
                tickets.size(), commits.size());
        return tickets;
    }

    /**
     * Returns all Jira ticket IDs found in the given text.
     *
     * @param text the raw text to scan
     * @return ordered list of ticket IDs (may be empty)
     */
    public List<String> extractIds(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> ids = new ArrayList<>();
        Matcher matcher = JIRA_PATTERN.matcher(text);
        while (matcher.find()) {
            String id = matcher.group(1);
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }
}
