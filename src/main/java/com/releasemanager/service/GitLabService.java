package com.releasemanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.releasemanager.config.AppConfig;
import com.releasemanager.model.CommitInfo;
import com.releasemanager.util.RetryUtil;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Communicates with the GitLab REST API to retrieve commit differences
 * between two branches.
 */
public class GitLabService {

    private static final Logger log = LoggerFactory.getLogger(GitLabService.class);
    private static final int PAGE_SIZE = 100;

    private final AppConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GitLabService(AppConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    /**
     * Returns all commits that are in {@code sourceBranch} but not yet in
     * {@code targetBranch} (i.e. the branch diff).
     *
     * @param sourceBranch branch to compare from
     * @param targetBranch branch to compare to
     * @return list of {@link CommitInfo} objects, newest first
     * @throws Exception on network or API errors
     */
    public List<CommitInfo> getCommitDiff(String sourceBranch, String targetBranch)
            throws Exception {

        log.info("Fetching commit diff: {} → {}", sourceBranch, targetBranch);
        List<CommitInfo> commits = new ArrayList<>();
        int page = 1;

        while (true) {
            final int currentPage = page;
            String responseBody = RetryUtil.execute(
                    () -> fetchPage(sourceBranch, targetBranch, currentPage),
                    "GitLab-compare-page-" + currentPage
            );

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode commitsNode = root.path("commits");

            if (!commitsNode.isArray() || commitsNode.isEmpty()) break;

            for (JsonNode node : commitsNode) {
                commits.add(mapCommit(node));
            }

            if (commitsNode.size() < PAGE_SIZE) break;
            page++;
        }

        log.info("Total commits found: {}", commits.size());
        return commits;
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private String fetchPage(String from, String to, int page) throws IOException {
        String encodedProject = config.getProjectId();
        HttpUrl url = Objects.requireNonNull(
                HttpUrl.parse(config.getGitlabUrl()
                        + "/api/v4/projects/" + encodedProject + "/repository/compare"))
                .newBuilder()
                .addQueryParameter("from", to)
                .addQueryParameter("to", from)
                .addQueryParameter("per_page", String.valueOf(PAGE_SIZE))
                .addQueryParameter("page", String.valueOf(page))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("PRIVATE-TOKEN", config.getGitlabToken())
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("GitLab API error " + response.code()
                        + " for URL: " + url);
            }
            return Objects.requireNonNull(response.body()).string();
        }
    }

    private CommitInfo mapCommit(JsonNode node) {
        return new CommitInfo(
                node.path("id").asText(),
                node.path("message").asText(),
                node.path("author_name").asText(),
                node.path("author_email").asText(),
                parseDate(node.path("committed_date").asText(null))
        );
    }

    private OffsetDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return OffsetDateTime.parse(raw);
        } catch (Exception e) {
            log.warn("Could not parse date '{}': {}", raw, e.getMessage());
            return null;
        }
    }
}
