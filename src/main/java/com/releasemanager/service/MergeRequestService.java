package com.releasemanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.releasemanager.config.AppConfig;
import com.releasemanager.model.JiraTicket;
import com.releasemanager.model.MergeRequestResult;
import com.releasemanager.util.RetryUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Creates (or retrieves an existing) GitLab Merge Request for the configured
 * source → target branch pair.
 */
public class MergeRequestService {

    private static final Logger log = LoggerFactory.getLogger(MergeRequestService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final AppConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MergeRequestService(AppConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Creates a new Merge Request, or returns the first existing open MR for
     * the same source/target branch pair if one already exists.
     *
     * @param tickets  Jira tickets to include in the MR description
     * @param commitCount total number of commits in the diff
     * @return {@link MergeRequestResult} with MR metadata
     * @throws Exception on network or API errors
     */
    public MergeRequestResult createOrGet(List<JiraTicket> tickets, int commitCount)
            throws Exception {

        String source = config.getSourceBranch();
        String target = config.getTargetBranch();

        // Check for an existing open MR first
        MergeRequestResult existing = findOpenMR(source, target);
        if (existing != null) {
            log.info("Found existing open MR !{}: {}", existing.iid(), existing.url());
            return existing;
        }

        // Build description
        List<String> jiraIds = tickets.stream()
                .map(JiraTicket::ticketId)
                .distinct()
                .collect(Collectors.toList());

        String description = buildDescription(jiraIds, commitCount);
        String title = "Release: " + source + " → " + target;

        return RetryUtil.execute(
                () -> postMR(title, description, source, target, jiraIds, commitCount),
                "CreateMergeRequest"
        );
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private MergeRequestResult findOpenMR(String source, String target) throws IOException {
        String url = config.getGitlabUrl() + "/api/v4/projects/" + config.getProjectId()
                + "/merge_requests?state=opened&source_branch=" + source
                + "&target_branch=" + target;

        Request request = new Request.Builder()
                .url(url)
                .header("PRIVATE-TOKEN", config.getGitlabToken())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            JsonNode array = objectMapper.readTree(
                    Objects.requireNonNull(response.body()).string());
            if (array.isArray() && !array.isEmpty()) {
                JsonNode mr = array.get(0);
                return new MergeRequestResult(
                        mr.path("web_url").asText(),
                        mr.path("iid").asInt(),
                        mr.path("title").asText(),
                        source, target, 0, 0, List.of(), false
                );
            }
        }
        return null;
    }

    private MergeRequestResult postMR(
            String title, String description,
            String source, String target,
            List<String> jiraIds, int commitCount) throws IOException {

        ObjectNode body = objectMapper.createObjectNode();
        body.put("title", title);
        body.put("source_branch", source);
        body.put("target_branch", target);
        body.put("description", description);
        body.put("remove_source_branch", false);

        RequestBody requestBody = RequestBody.create(body.toString(), JSON);
        Request request = new Request.Builder()
                .url(config.getGitlabUrl() + "/api/v4/projects/"
                        + config.getProjectId() + "/merge_requests")
                .header("PRIVATE-TOKEN", config.getGitlabToken())
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to create MR: HTTP " + response.code());
            }
            JsonNode mr = objectMapper.readTree(
                    Objects.requireNonNull(response.body()).string());
            log.info("Created MR !{}: {}", mr.path("iid").asInt(),
                    mr.path("web_url").asText());
            return new MergeRequestResult(
                    mr.path("web_url").asText(),
                    mr.path("iid").asInt(),
                    title, source, target,
                    commitCount, jiraIds.size(), jiraIds, true
            );
        }
    }

    private String buildDescription(List<String> jiraIds, int commitCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Release Summary\n\n");
        sb.append("- **Total commits**: ").append(commitCount).append("\n");
        sb.append("- **Jira tickets**: ").append(jiraIds.size()).append("\n\n");
        if (!jiraIds.isEmpty()) {
            sb.append("### Jira Tickets\n\n");
            jiraIds.forEach(id -> sb.append("- ").append(id).append("\n"));
        }
        return sb.toString();
    }
}
