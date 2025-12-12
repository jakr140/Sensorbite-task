# CI/CD Workflows

This directory contains GitHub Actions workflows for continuous integration and deployment.

## Workflows

### build.yml - Main Build Pipeline
**Triggers:** Push and Pull Requests to `main` and `develop` branches

**Jobs:**
1. **build-and-test** - Builds the Maven project and runs all tests
   - Uses Java 21 (Temurin distribution)
   - Caches Maven dependencies for faster builds
   - Runs `mvn clean verify`
   - Uploads test results and build artifacts

2. **docker-build** - Validates Docker image
   - Builds Docker image using multi-stage Dockerfile
   - Starts container and verifies health check
   - Uses GitHub Actions cache for faster rebuilds

3. **test-results-summary** - Test reporting
   - Publishes test results summary on pull requests
   - Provides quick visibility into test failures

**Artifacts:**
- `test-results`: JUnit XML reports (retained 30 days)
- `build-artifacts`: JAR files (retained 7 days)

### dependency-scan.yml - Security Scanning
**Triggers:**
- Weekly schedule (Monday 00:00 UTC)
- Manual dispatch
- Pull requests to `main` that modify pom.xml files

**Jobs:**
1. **dependency-check** - OWASP Dependency Check
   - Scans for known vulnerabilities in dependencies
   - Fails build if CVSS score >= 7
   - Uploads HTML and JSON reports

2. **snyk-scan** - Snyk vulnerability scanning
   - Requires `SNYK_TOKEN` secret to be configured
   - Uploads results to GitHub Security tab
   - Continues on error to avoid blocking builds

**Configuration:**
- Suppression file: `.github/dependency-suppression.xml`

## Setup

### Required Secrets
None required for basic functionality.

### Optional Secrets
- `SNYK_TOKEN`: Enable Snyk security scanning (get from https://snyk.io)

## Adding JaCoCo Code Coverage

To enable code coverage tracking, add JaCoCo plugin to the parent `pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Coverage reports will be automatically included in workflow artifacts.

## Local Testing

Test the workflows locally using [act](https://github.com/nektos/act):

```bash
# Run build workflow
act push --workflows .github/workflows/build.yml

# Run dependency scan
act workflow_dispatch --workflows .github/workflows/dependency-scan.yml
```

## Workflow Status

Check workflow runs in the [Actions tab](../../actions).
