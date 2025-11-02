# Implementing Build System Logic

This guide outlines the process for creating and maintaining custom Gradle plugins and build logic in the JabRef project, including plugin development, testing, and integration.

## Build Logic Architecture

JabRef uses a modular build system with custom plugins:

### Plugin Categories

- **Code Quality**: Linting, formatting, static analysis
- **Testing**: Test execution, coverage, integration
- **Packaging**: JAR creation, native binaries, installers
- **Publication**: Artifact publishing, repository management
- **Development**: IDE integration, hot reloading, debugging

### Directory Structure

```
build-logic/
├── src/main/kotlin/           # Plugin implementations
│   ├── jabref.build.quality/  # Code quality plugins
│   ├── jabref.build.testing/  # Testing plugins
│   └── jabref.build.utils/    # Utility plugins
├── src/test/kotlin/           # Plugin tests
└── build.gradle.kts           # Plugin build configuration
```

## Plugin Development Process

### 1. Plugin Planning

- Define the build automation need
- Identify target projects (jablib, jabgui, etc.)
- Determine configuration options and defaults
- Plan integration with existing build pipeline

### 2. Plugin Implementation

Create plugin class following Gradle conventions:

```kotlin
// build-logic/src/main/kotlin/jabref/build/JabRefPlugin.kt
package jabref.build

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*

class JabRefPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Apply base plugins
        project.pluginManager.apply("java")
        project.pluginManager.apply("jacoco")

        // Configure repositories
        project.repositories {
            mavenCentral()
            gradlePluginPortal()
        }

        // Add custom tasks
        project.tasks.register("customTask", CustomTask::class.java)

        // Configure extensions
        val extension = project.extensions.create("jabref", JabRefExtension::class.java)
        configureDefaults(extension)

        // Apply conventions
        configureJava(project)
        configureTesting(project)
        configurePublishing(project)
    }

    private fun configureDefaults(extension: JabRefExtension) {
        extension.apply {
            javaVersion.set(17)
            enablePreview.set(false)
            testParallelism.set(Runtime.getRuntime().availableProcessors())
        }
    }

    private fun configureJava(project: Project) {
        project.java {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    private fun configureTesting(project: Project) {
        project.tasks.withType<Test> {
            useJUnitPlatform()
            maxParallelForks = Runtime.getRuntime().availableProcessors()
            testLogging {
                events("passed", "skipped", "failed")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }

    private fun configurePublishing(project: Project) {
        // Publishing configuration
    }
}

// Extension for configuration
open class JabRefExtension @Inject constructor(objects: ObjectFactory) {
    val javaVersion: Property<Int> = objects.property(Int::class.java)
    val enablePreview: Property<Boolean> = objects.property(Boolean::class.java)
    val testParallelism: Property<Int> = objects.property(Int::class.java)
}
```

### 3. Custom Tasks

Implement custom Gradle tasks when needed:

```kotlin
// build-logic/src/main/kotlin/jabref/build/tasks/CustomTask.kt
package jabref.build.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class CustomTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val configuration: Property<String>

    @TaskAction
    fun execute() {
        val workQueue = workerExecutor.noIsolation()

        workQueue.submit(CustomWorkAction::class.java) { parameters ->
            parameters.inputDir.set(inputDir)
            parameters.outputDir.set(outputDir)
            parameters.configuration.set(configuration)
        }
    }
}

// Work action for parallel execution
abstract class CustomWorkAction : WorkAction<CustomWorkParameters> {
    override fun execute() {
        val inputDir = parameters.inputDir.get().asFile
        val outputDir = parameters.outputDir.get().asFile
        val config = parameters.configuration.get()

        // Task implementation
        processFiles(inputDir, outputDir, config)
    }
}

interface CustomWorkParameters : WorkParameters {
    val inputDir: DirectoryProperty
    val outputDir: DirectoryProperty
    val configuration: Property<String>
}
```

## Plugin Testing

### Unit Testing Plugins

```kotlin
// build-logic/src/test/kotlin/jabref/build/JabRefPluginTest.kt
package jabref.build

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JabRefPluginTest {

    @Test
    fun `plugin applies successfully`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build()

        project.pluginManager.apply(JabRefPlugin::class.java)

        // Verify plugin applied
        assertNotNull(project.tasks.findByName("customTask"))

        // Verify extensions created
        val extension = project.extensions.findByType(JabRefExtension::class.java)
        assertNotNull(extension)

        // Verify default values
        assertTrue(extension.javaVersion.get() == 17)
    }

    @Test
    fun `plugin configures Java correctly`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build()

        project.pluginManager.apply(JabRefPlugin::class.java)

        // Verify Java configuration
        assertTrue(project.java.sourceCompatibility == JavaVersion.VERSION_17)
        assertTrue(project.java.targetCompatibility == JavaVersion.VERSION_17)
    }
}
```

### Integration Testing

```kotlin
// build-logic/src/test/kotlin/jabref/build/integration/BuildIntegrationTest.kt
package jabref.build.integration

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertTrue

class BuildIntegrationTest {

    @Test
    fun `build succeeds with plugin applied`(@TempDir tempDir: Path) {
        // Create test project
        tempDir.resolve("build.gradle.kts").toFile().writeText("""
            plugins {
                id("jabref.build")
            }
        """)

        tempDir.resolve("src/main/java/Test.java").toFile().apply {
            parentFile.mkdirs()
            writeText("public class Test {}")
        }

        // Run build
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withArguments("build")
            .withPluginClasspath()
            .build()

        // Verify success
        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }
}
```

## Configuration and Conventions

### Convention Plugins

Create convention plugins for common configurations:

```kotlin
// build-logic/src/main/kotlin/jabref/build/conventions/JavaConventions.kt
package jabref.build.conventions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.*

class JavaConventions : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("java")

        project.java {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        project.tasks.withType<JavaCompile> {
            options.compilerArgs.addAll(listOf(
                "-Xlint:all",
                "-Xlint:-processing",
                "-Werror"
            ))
        }

        // Configure source sets
        project.sourceSets {
            main {
                java.srcDirs("src/main/java")
                resources.srcDirs("src/main/resources")
            }
            test {
                java.srcDirs("src/test/java")
                resources.srcDirs("src/test/resources")
            }
        }
    }
}
```

### Version Catalogs

Use version catalogs for dependency management:

```kotlin
// gradle/libs.versions.toml
[versions]
junit = "5.9.2"
mockito = "5.1.1"
guava = "31.1-jre"

[libraries]
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }
guava = { module = "com.google.guava:guava", version.ref = "guava" }

[plugins]
jabref-build = { id = "jabref.build", version = "1.0.0" }
```

## Performance Optimization

### Build Caching

```kotlin
// Enable build caching
tasks.withType<JavaCompile> {
    options.isIncremental = true
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Enable test caching
    outputs.cacheIf { true }
}
```

### Parallel Execution

```kotlin
// Configure parallel execution
project.gradle.startParameter.isParallelProjectExecutionEnabled = true

tasks.withType<Test> {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}
```

## Error Handling and Validation

### Plugin Validation

```kotlin
class PluginValidator {
    fun validate(project: Project) {
        // Check required plugins
        if (!project.pluginManager.hasPlugin("java")) {
            throw InvalidUserDataException("Java plugin must be applied before JabRef plugin")
        }

        // Validate configuration
        val extension = project.extensions.findByType(JabRefExtension::class.java)
            ?: throw InvalidUserDataException("JabRef extension not found")

        if (extension.javaVersion.get() < 11) {
            throw InvalidUserDataException("Java version must be 11 or higher")
        }
    }
}
```

### Build Failure Handling

```kotlin
tasks.register("validateBeforeBuild") {
    doLast {
        // Pre-build validation
        validateProject()
    }
}

// Make it a dependency of build
tasks.named("build") {
    dependsOn("validateBeforeBuild")
}
```

## Integration with CI/CD

### CI-Specific Configuration

```kotlin
// Conditional configuration based on environment
if (project.hasProperty("ci")) {
    tasks.withType<Test> {
        // CI-specific test configuration
        maxParallelForks = 2  // Limit parallelism in CI
        testLogging.showStackTraces = true
    }
}
```

### Artifact Publishing

```kotlin
plugins.apply("maven-publish")

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("JabRef Build Logic")
                description.set("Custom Gradle plugins for JabRef")
            }
        }
    }
}
```

## Documentation and Maintenance

### Plugin Documentation

```kotlin
/**
 * JabRef Build Plugin
 *
 * This plugin applies JabRef-specific build conventions including:
 * - Java 17 compatibility
 * - Testing configuration with JUnit 5
 * - Code quality checks
 * - Publishing configuration
 *
 * Usage:
 * ```kotlin
 * plugins {
 *     id("jabref.build")
 * }
 *
 * jabref {
 *     javaVersion.set(21)
 *     enablePreview.set(true)
 * }
 * ```
 */
class JabRefPlugin : Plugin<Project> {
    // Implementation
}
```

### Version Management

```kotlin
object Versions {
    const val JUNIT = "5.9.2"
    const val MOCKITO = "5.1.1"
    const val JACOCO = "0.8.8"

    // Plugin versions
    const val PLUGIN_VERSION = "1.0.0"
}
```

## Testing Build Logic

### Functional Testing

```kotlin
@Test
fun `plugin creates expected tasks`(@TempDir tempDir: Path) {
    val project = createProject(tempDir)

    project.pluginManager.apply(JabRefPlugin::class.java)

    // Verify tasks exist
    assertNotNull(project.tasks.findByName("customTask"))
    assertNotNull(project.tasks.findByName("jacocoTestReport"))
}

@Test
fun `extension configures defaults`(@TempDir tempDir: Path) {
    val project = createProject(tempDir)

    project.pluginManager.apply(JabRefPlugin::class.java)

    val extension = project.extensions.getByType(JabRefExtension::class.java)

    assertEquals(17, extension.javaVersion.get())
    assertFalse(extension.enablePreview.get())
}
```
