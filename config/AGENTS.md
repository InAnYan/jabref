# Managing Configuration and Code Quality

This guide outlines the process for managing configuration files, code quality tools, and development environment setup in the JabRef project.

## Configuration Categories

JabRef maintains various configuration files for different purposes:

### Code Quality Tools

- **Checkstyle**: Java code style and formatting rules
- **SpotBugs**: Static analysis for bug detection
- **PMD**: Source code analyzer for common programming flaws
- **JaCoCo**: Code coverage reporting

### Development Environment

- **Eclipse**: IDE-specific code style settings
- **IntelliJ IDEA**: IDE-specific configuration
- **VSCode**: Editor configuration for development

### Build and CI/CD

- **GitHub Actions**: CI/CD pipeline configuration
- **CodeQL**: Security vulnerability scanning
- **Dependabot**: Automated dependency updates

## Code Quality Tool Management

### Checkstyle Configuration

#### Rule Customization Process

1. **Identify Issue**: Determine what code style issue needs addressing
2. **Research Best Practices**: Review Java coding standards and JabRef conventions
3. **Test Rule Change**: Apply rule to existing codebase and review violations
4. **Update Configuration**: Modify `checkstyle.xml` with justified changes
5. **Document Rationale**: Update this guide with reasoning for rule changes

#### Example Rule Addition

```xml
<!-- config/checkstyle/checkstyle.xml -->
<module name="Checker">
    <!-- Existing configuration -->

    <!-- New rule for method parameter naming -->
    <module name="MethodParamPad">
        <property name="allowLineBreaks" value="true"/>
    </module>
</module>
```

#### Suppressions Management

```xml
<!-- config/checkstyle/suppressions.xml -->
<suppressions>
    <!-- Suppress specific violations with justification -->
    <suppress checks="MethodParamPad"
              files=".*[/\\]generated[/\\].*"
              message="Generated code should not be modified"/>
</suppressions>
```

### Quality Gate Configuration

#### CI/CD Integration

```yaml
# .github/workflows/ci.yml
- name: Code Quality Checks
  run: |
    ./gradlew checkstyleMain checkstyleTest
    ./gradlew spotbugsMain
    ./gradlew pmdMain

- name: Upload Reports
  uses: actions/upload-artifact@v3
  with:
    name: quality-reports
    path: |
      build/reports/checkstyle/
      build/reports/spotbugs/
      build/reports/pmd/
```

#### Quality Thresholds

```kotlin
// build.gradle.kts
checkstyle {
    toolVersion = "10.12.4"
    configFile = file("${rootProject.projectDir}/config/checkstyle/checkstyle.xml")
    configFile.file // Validate config exists
}

spotbugs {
    toolVersion = "4.8.3"
    excludeFilter = file("${rootProject.projectDir}/config/checkstyle/spotbugs-excludes.xml")
}

jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/model/**',  // Exclude model classes
                '**/config/**'  // Exclude configuration
            ])
        }))
    }
}
```

## IDE Configuration Management

### Eclipse Setup

```xml
<!-- config/Eclipse Code Style.epf -->
file_export_version=3.0
/instance/org.eclipse.jdt.core/org.eclipse.jdt.core.formatter.profile=_JabRef
/instance/org.eclipse.jdt.core/org.eclipse.jdt.core.formatter.profile_name=JabRef
```

### IntelliJ IDEA Setup

```xml
<!-- config/VSCode Code Style.xml -->
<code_scheme name="JabRef">
    <JavaCodeStyleSettings>
        <option name="CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND" value="20"/>
        <option name="NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND" value="3"/>
    </JavaCodeStyleSettings>
</code_scheme>
```

### VSCode Setup

```json
// .vscode/settings.json
{
    "java.format.settings.url": "./config/VSCode Code Style.xml",
    "java.format.settings.profile": "JabRef",
    "editor.formatOnSave": true,
    "editor.codeActionsOnSave": {
        "source.organizeImports": true
    }
}
```

## Configuration Validation

### Automated Validation

```bash
#!/bin/bash
# config/validate-config.sh

validate_checkstyle() {
    echo "Validating Checkstyle configuration..."
    if ! xmllint --noout config/checkstyle/checkstyle.xml; then
        echo "ERROR: Invalid Checkstyle XML"
        exit 1
    fi
}

validate_spotbugs() {
    echo "Validating SpotBugs exclusions..."
    if ! xmllint --noout config/checkstyle/spotbugs-excludes.xml; then
        echo "ERROR: Invalid SpotBugs XML"
        exit 1
    fi
}

# Run all validations
validate_checkstyle
validate_spotbugs
echo "All configurations are valid"
```

### Configuration Testing

```kotlin
// config/src/test/kotlin/ConfigValidationTest.kt
class ConfigValidationTest {

    @Test
    fun `checkstyle config is valid XML`() {
        val configFile = File("config/checkstyle/checkstyle.xml")
        assertTrue(configFile.exists(), "Checkstyle config file should exist")

        // Parse XML to validate structure
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        assertDoesNotThrow { documentBuilder.parse(configFile) }
    }

    @Test
    fun `checkstyle rules are reasonable`() {
        val configFile = File("config/checkstyle/checkstyle.xml")

        // Parse and validate rule counts
        val rules = parseCheckstyleRules(configFile)
        assertTrue(rules.size > 10, "Should have reasonable number of rules")
        assertTrue(rules.any { it.contains("indentation") }, "Should check indentation")
    }
}
```

## Tool Version Management

### Version Catalogs

```toml
# gradle/libs.versions.toml
[versions]
checkstyle = "10.12.4"
spotbugs = "4.8.3"
pmd = "6.55.0"
jacoco = "0.8.8"

[plugins]
checkstyle = { id = "checkstyle", version.ref = "checkstyle" }
spotbugs = { id = "com.github.spotbugs", version.ref = "spotbugs" }
pmd = { id = "pmd", version.ref = "pmd" }
jacoco = { id = "jacoco", version.ref = "jacoco" }
```

### Version Update Process

1. **Monitor Updates**: Check for new versions of quality tools
2. **Test Compatibility**: Run tools against codebase with new versions
3. **Update Configuration**: Modify version catalogs and test configurations
4. **Validate Changes**: Ensure no new violations are introduced
5. **Document Changes**: Update changelog and migration guides

## Rule Customization Guidelines

### When to Add Rules

- **Enforce Consistency**: Rules that prevent common mistakes
- **Improve Readability**: Rules that make code more maintainable
- **Prevent Bugs**: Rules that catch potential issues early
- **Follow Standards**: Rules that align with Java best practices

### When to Modify Rules

- **False Positives**: When rules flag correct code
- **Legacy Code**: When rules conflict with existing patterns
- **Tool Limitations**: When rules don't work as expected
- **Performance Impact**: When rules significantly slow down builds

### Rule Justification Requirements

Every rule change must include:
- **Problem Statement**: What issue does this rule address?
- **Impact Assessment**: How many files will be affected?
- **Alternative Solutions**: Why this rule over other approaches?
- **Maintenance Cost**: Ongoing effort to maintain this rule

## Integration Testing

### Quality Tool Integration Tests

```kotlin
// config/src/test/kotlin/QualityIntegrationTest.kt
class QualityIntegrationTest {

    @Test
    fun `checkstyle passes on main source code`() {
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("checkstyleMain")
            .build()

        assertEquals(SUCCESS, result.task(":checkstyleMain")?.outcome)
    }

    @Test
    fun `code coverage meets minimum threshold`() {
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("jacocoTestReport")
            .build()

        val coverageReport = File("build/reports/jacoco/test/html/index.html")
        assertTrue(coverageReport.exists())

        // Parse coverage percentage
        val coverage = parseCoveragePercentage(coverageReport)
        assertTrue(coverage >= 80.0, "Code coverage should be at least 80%, was: $coverage")
    }
}
```

## Documentation and Maintenance

### Configuration Documentation

```markdown
# Code Quality Configuration

## Checkstyle Rules

### Modified Rules
- **Indentation**: Modified to allow 4-space indentation
  - **Rationale**: Aligns with project coding standards
  - **Impact**: Affects ~50 files
  - **Date**: 2024-01-15

### Suppressed Rules
- **MethodParamPad**: Suppressed for generated code
  - **Rationale**: Generated code should not be modified
  - **Files**: `build/generated/**/*`
  - **Date**: 2024-02-01
```

### Maintenance Schedule

- **Weekly**: Review CI/CD quality check results
- **Monthly**: Update tool versions and test compatibility
- **Quarterly**: Audit rule effectiveness and false positive rates
- **Annually**: Complete configuration review and cleanup

## Troubleshooting

### Common Issues

#### Checkstyle Configuration Errors

```bash
# Validate XML syntax
xmllint --noout config/checkstyle/checkstyle.xml

# Test configuration against sample file
java -cp checkstyle.jar com.puppycrawl.tools.checkstyle.Main \
     -c config/checkstyle/checkstyle.xml \
     src/main/java/Sample.java
```

#### Performance Issues

- **Large Codebase**: Use incremental analysis where possible
- **Complex Rules**: Simplify or remove performance-intensive rules
- **Parallel Execution**: Configure tools to use multiple threads

#### False Positives

- **Document Exceptions**: Add suppressions with clear justification
- **Rule Tuning**: Adjust rule parameters to reduce false positives
- **Custom Rules**: Implement project-specific rules when needed
