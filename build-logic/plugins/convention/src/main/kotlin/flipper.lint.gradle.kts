import dev.detekt.gradle.Detekt

plugins {
    id("dev.detekt")
}

tasks.register<Detekt>("detektFormat") {
    autoCorrect = true
    ignoreFailures = true
    // autoCorrect mutates sources (inputs); caching would restore reports without reformatting
    outputs.cacheIf { false }
}

tasks.withType<Detekt> {
    reports {
        html.required.set(true)
        checkstyle.required.set(false)
        sarif.required.set(false)
        markdown.required.set(false)
    }

    setSource(files(projectDir))
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))

    include("**/*.kt", "**/*.kts")
    exclude(
        "**/resources/**",
        "**/build/**",
    )

    exclude {
        it.file.absolutePath.contains("/build/generated/")
    }

    parallel = true

    buildUponDefaultConfig = true

    allRules = true
}

dependencies {
    detektPlugins(libs.detekt.ruleset.ktlint)
    detektPlugins(libs.detekt.ruleset.compose)
    detektPlugins(libs.detekt.ruleset.decompose)
}
