/**
 * Opt into build caching for AGP/Compose tasks that are not cached by default for
 * "not worth it" reasons (see AGP DisabledCachingReason),
 * but only where outputs are expected to stay small (well under ~20MB) so the cache
 * is not filled with huge throw-away artifacts.
 *
 * Measured ceilings: jars/aars ~0.1–1.7MB, compose resource dirs ~2–4MB,
 * library class dirs ~2.2MB, host-test unit APKs ~2.2–2.7MB, app merge assets/native ~2.9MB.
 *
 * Intentionally NOT cached: final package{Debug,Internal,Release}* APKs and
 * top-level bundle{Debug,Internal,Release} AABs (tens of MB+ and invalidated by almost any change).
 */
tasks.configureEach {
    if (isSmallOutputBuildCacheCandidate(name)) {
        outputs.cacheIf { true }
    }
}

fun isSmallOutputBuildCacheCandidate(name: String): Boolean {
    if (isLargeFinalArtifactTask(name)) {
        return false
    }

    // Tiny metadata / models / java-res / jars / desktop (KB–low MB)
    val exact = setOf(
        "writeAndroidMainAarMetadata",
        "generateAndroidMainEmptyResourceFiles",
        "mapAndroidMainSourceSetPaths",
        "checkAndroidMainAarMetadata",
        "prepareLintJarForPublish",
        "processAndroidMainJavaRes",
        "mergeAndroidMainJavaResource",
        "generateAndroidHostTestLintModel",
        "mapDebugSourceSetPaths",
        "generateDebugLintReportModel",
        "generateDebugUnitTestLintModel",
        "generateDebugAndroidTestLintModel",
        "extractProguardFiles",
        "createDebugCompatibleScreenManifests",
        "checkDebugAarMetadata",
        "bundleAndroidMainLocalLintAar",
        "createFullJarAndroidMain",
        "bundleAndroidMainClassesToRuntimeJar",
        "bundleAndroidMainClassesToCompileJar",
        "bundleDebugClassesToCompileJar",
        "desktopJar",
        "desktopProcessResources",
        "desktopMainClasses",
        "assembleDesktopMainResources",
        // Host unit-test packaging (CI allTests / testAndroidHostTest)
        "androidJar",
        "packageAndroidHostTestForUnitTest",
        "generateAndroidHostTestConfig",
        "processAndroidHostTestManifest",
        "mergeAndroidHostTestAssets",
        "mapAndroidHostTestSourceSetPaths",
        "checkAndroidHostTestAarMetadata",
        // Desktop unit-test packaging (CI desktopTest / allTests)
        "desktopTestClasses",
        "desktopTestProcessResources",
        "assembleDesktopTestResources",
    )

    // Compose resource codegen / prepare (KB–low MB dirs; theme fonts ~2–4MB)
    val prefixes = listOf(
        "generateComposeResClass",
        "generateExpectResourceCollectors",
        "generateActualResourceCollectors",
        "copyAndroidMainComposeResources",
        "prepareComposeResources",
        "generateResourceAccessors",
        "convertXmlValueResources",
        "copyNonXmlValueResources",
    )

    return name in exact ||
        prefixes.any { name.startsWith(it) } ||
        isSmallVariantPackagingTask(name)
}

/** Final APK/AAB packaging — exceeds ~20MB size (e.g. app-debug.apk ~51MB). */
fun isLargeFinalArtifactTask(name: String): Boolean {
    // packageDebug, packageInternalUniversalApk, packageReleaseBundle, …
    // Does NOT match packageAndroidHostTestForUnitTest or packageAndroidMainResources.
    if (name.matches(Regex("^package(Debug|Internal|Release|NonMinified|Benchmark).*$"))) {
        return true
    }
    // Top-level AAB tasks only (not bundleAndroidMain*, bundleLib*, bundle*Classes*).
    if (name.matches(Regex("^bundle(Debug|Internal|Release)$"))) {
        return true
    }
    return false
}

/**
 * Variant-agnostic AGP helpers (Debug / Internal / Release / NonMinified / Benchmark, …).
 * Same work as the measured Debug tasks; outputs stay KB–low MB.
 */
fun isSmallVariantPackagingTask(name: String): Boolean {
    return (name.startsWith("write") && name.endsWith("AppMetadata")) ||
        (name.startsWith("write") && name.endsWith("SigningConfigVersions")) ||
        name.startsWith("validateSigning") ||
        (name.startsWith("check") && name.endsWith("DuplicateClasses")) ||
        (name.startsWith("merge") && name.endsWith("JniLibFolders")) ||
        (name.startsWith("merge") && name.endsWith("JavaResource")) ||
        (name.startsWith("merge") && name.endsWith("Assets")) ||
        (name.startsWith("merge") && name.endsWith("NativeLibs")) ||
        (name.startsWith("process") && name.endsWith("JavaRes")) ||
        (name.startsWith("strip") && name.endsWith("DebugSymbols")) ||
        (name.startsWith("create") && name.endsWith("ApkListingFileRedirect")) ||
        (name.startsWith("copy") && name.contains("JniLibsProjectOnly")) ||
        name.startsWith("bundleLibRuntimeToDir")
}
