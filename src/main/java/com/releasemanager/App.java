package com.releasemanager;

import com.releasemanager.config.AppConfig;
import com.releasemanager.model.CommitInfo;
import com.releasemanager.model.JiraTicket;
import com.releasemanager.model.MergeRequestResult;
import com.releasemanager.model.ReportOutput;
import com.releasemanager.service.*;
import com.releasemanager.util.AppLogger;
import org.slf4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Application entry point.
 *
 * <p>Run with {@code java -jar gitlab-release-manager.jar} (uses values from
 * {@code .env}) or override via CLI flags:
 * <pre>
 *   --source develop --target staging
 * </pre>
 */
@Command(
        name        = "gitlab-release-manager",
        mixinStandardHelpOptions = true,
        version     = "1.0.0",
        description = "Compare GitLab branches, extract Jira tickets, create an MR, "
                    + "and generate reports."
)
public class App implements Callable<Integer> {

    private static final Logger log = AppLogger.getLogger(App.class);

    @Option(names = {"--source", "-s"}, description = "Source branch (overrides .env)")
    private String sourceBranch;

    @Option(names = {"--target", "-t"}, description = "Target branch (overrides .env)")
    private String targetBranch;

    @Option(names = {"--output", "-o"}, description = "Output directory for reports")
    private String outputDir;

    @Option(names = {"--dry-run"}, description = "Skip MR creation, only generate reports")
    private boolean dryRun;

    // ── Entry point ────────────────────────────────────────────────────────

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    // ── Callable ───────────────────────────────────────────────────────────

    @Override
    public Integer call() throws Exception {
        AppConfig config = AppConfig.getInstance();

        // CLI overrides
        String src = sourceBranch != null ? sourceBranch : config.getSourceBranch();
        String tgt = targetBranch != null ? targetBranch : config.getTargetBranch();
        String out = outputDir    != null ? outputDir    : config.getOutputDir();

        AppLogger.ensureLogDirectory(config.getLogDir());
        log.info("=== GitLab Release Manager starting ===");
        log.info("Source: {}  Target: {}  Output: {}  DryRun: {}", src, tgt, out, dryRun);

        // 1. Fetch commits
        GitLabService gitLab = new GitLabService(config);
        List<CommitInfo> commits = gitLab.getCommitDiff(src, tgt);
        log.info("Commits in diff: {}", commits.size());

        // 2. Extract Jira tickets
        JiraParser parser = new JiraParser();
        List<JiraTicket> tickets = parser.parse(commits);
        log.info("Jira tickets found: {}", tickets.size());

        // 3. Create / retrieve Merge Request
        MergeRequestResult mr;
        if (dryRun) {
            log.info("Dry-run mode: skipping MR creation.");
            mr = new MergeRequestResult(
                    "(dry-run)", 0, "(dry-run)", src, tgt,
                    commits.size(), tickets.size(),
                    tickets.stream()
                           .map(JiraTicket::ticketId)
                           .distinct().toList(),
                    false
            );
        } else {
            MergeRequestService mrService = new MergeRequestService(config);
            mr = mrService.createOrGet(tickets, commits.size());
        }

        // 4. Generate reports
        ReportGenerator generator = new ReportGenerator(out);
        ReportOutput report = generator.generate(tickets, mr);

        log.info("=== Done ===");
        log.info("CSV   : {}", report.csvPath());
        log.info("Excel : {}", report.excelPath());
        log.info("JSON  : {}", report.jsonPath());

        System.out.println("\nReports generated:");
        System.out.println("  CSV   : " + report.csvPath());
        System.out.println("  Excel : " + report.excelPath());
        System.out.println("  JSON  : " + report.jsonPath());
        if (!dryRun) {
            System.out.println("  MR    : " + mr.url());
        }
        return 0;
    }
}
