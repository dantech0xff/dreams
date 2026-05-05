/**
 * Gradle task template for CI validation of hardcoded design values.
 *
 * Add this to your module's build.gradle.kts or create a convention plugin.
 *
 * Usage:
 *   ./gradlew validateDesignTokens
 *   ./gradlew validateDesignTokens -PtokenSeverity=error
 */

// --- Copy this into your build.gradle.kts ---

tasks.register("validateDesignTokens") {
    group = "verification"
    description = "Scan Kotlin files for hardcoded design values (Color, Dp, Sp)."

    doLast {
        val srcDir = layout.projectDirectory.dir("src/main/kotlin")
        if (!srcDir.asFile.exists()) {
            logger.warn("Source directory not found: ${srcDir.asFile.path}")
            return@doLast
        }

        val severity = project.findProperty("tokenSeverity")?.toString()

        // Patterns to detect
        val patterns = listOf(
            Triple("Color hex", Regex("""Color\(0x[0-9A-Fa-f]+\)"""), "error"),
            Triple("Color named", Regex("""(?<!\w)Color\.(Black|White|Red|Green|Blue|Yellow)"""), "warning"),
            Triple("Dp hardcode", Regex("""(?<!\w)\d+\.dp\b"""), "warning"),
            Triple("Sp hardcode", Regex("""(?<!\w)\d+\.sp\b"""), "warning"),
            Triple("Shape hardcode", Regex("""RoundedCornerShape\(\d+"""), "warning"),
        )

        // Excluded file patterns
        val excludePatterns = listOf(
            Regex("""Primitive.*\.kt$"""),
            Regex("""Token.*\.kt$"""),
            Regex("""Theme.*\.kt$"""),
            Regex("""Test\.kt$"""),
        )

        var totalViolations = 0
        var filesScanned = 0

        srcDir.asFile.walkTopDown()
            .filter { it.extension == "kt" }
            .filter { file -> excludePatterns.none { it.containsMatchIn(file.name) } }
            .forEach { file ->
                filesScanned++
                val lines = file.readLines()
                lines.forEachIndexed { index, line ->
                    if ("// noinspection DesignToken" in line) return@forEachIndexed
                    patterns.forEach { (name, regex, sev) ->
                        if (severity != null && sev != severity) return@forEach
                        if (regex.containsMatchIn(line)) {
                            logger.warn("${file.relativeTo(projectDir)}:${index + 1} [$sev] $name")
                            totalViolations++
                        }
                    }
                }
            }

        logger.lifecycle("Design token validation: $totalViolations violations in $filesScanned files")
        if (totalViolations > 0) {
            throw GradleException("Found $totalViolations hardcoded design values. Use design tokens instead.")
        }
    }
}

// --- Optional: Add to check lifecycle ---
// tasks.named("check") { dependsOn("validateDesignTokens") }
