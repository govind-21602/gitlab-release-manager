package com.releasemanager.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads application configuration from a {@code .env} file and environment
 * variables. Exposes a singleton via {@link #getInstance()}.
 *
 * <p>Required variables:
 * <ul>
 *   <li>GITLAB_URL</li>
 *   <li>GITLAB_TOKEN</li>
 *   <li>PROJECT_ID</li>
 *   <li>SOURCE_BRANCH</li>
 *   <li>TARGET_BRANCH</li>
 * </ul>
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static volatile AppConfig instance;

    private final String gitlabUrl;
    private final String gitlabToken;
    private final String projectId;
    private final String sourceBranch;
    private final String targetBranch;
    private final String outputDir;
    private final String logDir;

    private AppConfig() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        this.gitlabUrl    = resolve(dotenv, "GITLAB_URL");
        this.gitlabToken  = resolve(dotenv, "GITLAB_TOKEN");
        this.projectId    = resolve(dotenv, "PROJECT_ID");
        this.sourceBranch = resolve(dotenv, "SOURCE_BRANCH", "develop");
        this.targetBranch = resolve(dotenv, "TARGET_BRANCH", "staging");
        this.outputDir    = resolve(dotenv, "OUTPUT_DIR",    "reports/");
        this.logDir       = resolve(dotenv, "LOG_DIR",       "logs/");

        validate();
        log.info("Configuration loaded. GitLab URL: {}", gitlabUrl);
    }

    /**
     * Returns the singleton {@link AppConfig} instance (thread-safe).
     *
     * @return the application configuration
     */
    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    public String getGitlabUrl()    { return gitlabUrl; }
    public String getGitlabToken()  { return gitlabToken; }
    public String getProjectId()    { return projectId; }
    public String getSourceBranch() { return sourceBranch; }
    public String getTargetBranch() { return targetBranch; }
    public String getOutputDir()    { return outputDir; }
    public String getLogDir()       { return logDir; }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static String resolve(Dotenv dotenv, String key) {
        String value = dotenv.get(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return value == null ? "" : value.trim();
    }

    private static String resolve(Dotenv dotenv, String key, String defaultValue) {
        String value = resolve(dotenv, key);
        return value.isBlank() ? defaultValue : value;
    }

    private void validate() {
        List<String> missing = new ArrayList<>();
        if (gitlabUrl.isBlank())   missing.add("GITLAB_URL");
        if (gitlabToken.isBlank()) missing.add("GITLAB_TOKEN");
        if (projectId.isBlank())   missing.add("PROJECT_ID");

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "Missing required environment variables: " + String.join(", ", missing)
            );
        }
    }
}
