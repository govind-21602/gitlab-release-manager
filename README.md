# GitLab Release Manager

A Java 17 CLI tool that:

1. **Compares two GitLab branches** and retrieves the commit diff via the GitLab REST API.
2. **Extracts Jira ticket IDs** (e.g. `PROJ-123`) from every commit message.
3. **Creates a Merge Request** (or re-uses an existing open one) with a summary description.
4. **Generates reports** in CSV, Excel (`.xlsx`), and JSON formats.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java | 17+ |
| Maven | 3.8+ |
| GitLab PAT | `api` scope |

---

## Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/govind-21602/gitlab-release-manager.git
cd gitlab-release-manager

# 2. Configure environment
cp .env.example .env
# Edit .env with your GITLAB_URL, GITLAB_TOKEN, PROJECT_ID, SOURCE_BRANCH, TARGET_BRANCH

# 3. Build the fat JAR
mvn clean package -q

# 4. Run
java -jar target/gitlab-release-manager-1.0.0.jar
```

---

## CLI Options

```
Usage: gitlab-release-manager [-hV] [--dry-run] [-o=<outputDir>]
                               [-s=<sourceBranch>] [-t=<targetBranch>]
  -s, --source   Source branch (overrides .env SOURCE_BRANCH)
  -t, --target   Target branch (overrides .env TARGET_BRANCH)
  -o, --output   Output directory for reports  (overrides .env OUTPUT_DIR)
      --dry-run  Skip MR creation, only generate reports
  -h, --help     Show help message
  -V, --version  Print version
```

### Example

```bash
java -jar target/gitlab-release-manager-1.0.0.jar \
  --source feature/my-branch \
  --target main \
  --output /tmp/reports \
  --dry-run
```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/releasemanager/
│   │   ├── App.java                        # Entry point (PicoCLI)
│   │   ├── config/
│   │   │   └── AppConfig.java              # Dotenv + env-var config singleton
│   │   ├── model/
│   │   │   ├── CommitInfo.java
│   │   │   ├── JiraTicket.java
│   │   │   ├── MergeRequestResult.java
│   │   │   └── ReportOutput.java
│   │   ├── service/
│   │   │   ├── GitLabService.java          # GitLab API client
│   │   │   ├── JiraParser.java             # Regex-based Jira ID extractor
│   │   │   ├── MergeRequestService.java    # MR creation / lookup
│   │   │   └── ReportGenerator.java        # CSV / Excel / JSON writer
│   │   └── util/
│   │       ├── AppLogger.java
│   │       └── RetryUtil.java              # Exponential-backoff retry
│   └── resources/
│       └── logback.xml
└── test/
    └── java/com/releasemanager/service/
        ├── GitLabServiceTest.java
        ├── JiraParserTest.java
        ├── MergeRequestServiceTest.java
        └── ReportGeneratorTest.java
```

---

## Configuration Reference

| Variable | Required | Default | Description |
|---|---|---|---|
| `GITLAB_URL` | ✅ | — | GitLab base URL |
| `GITLAB_TOKEN` | ✅ | — | Personal Access Token |
| `PROJECT_ID` | ✅ | — | Numeric or encoded project ID |
| `SOURCE_BRANCH` | ❌ | `develop` | Branch to compare from |
| `TARGET_BRANCH` | ❌ | `staging` | Branch to compare to |
| `OUTPUT_DIR` | ❌ | `reports/` | Report output directory |
| `LOG_DIR` | ❌ | `logs/` | Log file directory |

---

## Running Tests

```bash
mvn test
```

---

## License

MIT
