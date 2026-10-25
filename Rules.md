# Rules - Photos
Last Updated: 2026-10-25

## Engineering
### R-001 - Strict Layered Architecture
Business logic, data access, and presentation must remain strictly separated. UI composables and activities must only observe ViewModels or state holders, and must never query Room DAOs, SQLite database directly, or execute image processing operations directly. Reason: Guarantees testability, isolation, and long-term maintainability. Example violation: Invoking database operations or bitmap transformations directly inside a Composable function.

### R-002 - Android Framework Naming and Placement Conventions
Framework-mandated files and directories (`app/`, `gradle/`, `build.gradle.kts`, `settings.gradle.kts`, `AndroidManifest.xml`, Kotlin packages under `com.arinara.photos`) are exempt from root PascalCase naming where build tooling requires standard Gradle conventions. Project-level folders created outside the Android module (`/Docs`, `/Changelog`, `/Assets`, `/Output`) must strictly use PascalCase. Reason: Prevents Gradle and Android build tooling breakages while preserving Arinara repository discipline. Example violation: Renaming `build.gradle.kts` to `BuildGradle.kts` or `app` to `App` causing build failures.

### R-003 - Offline-First Local Operations
All photo storage, album metadata indexing, on-device EXIF parsing, and media querying must function completely offline with zero mandatory network connectivity. Any failure in photo processing or metadata parsing must be caught, surfaced with user-friendly error states, and recorded in logs without swallowing exceptions. Reason: Users manage and view photos locally in offline scenarios and privacy-sensitive contexts. Example violation: Using an external cloud storage API or wrapping photo loading in an empty `catch (e: Exception) {}`.

### R-009 - Automated Android UI Rules Verification in Gradle
All Android.md design system rules (AR-001 through AR-016) must be enforced via the automated Gradle verification task `VerifyAndroidRulesTask`, bound directly to build and test lifecycles (`preBuild`, `testDebugUnitTest`, `check`). Builds must fail immediately if any component violates token adherence, border color restrictions, aspect ratios, hardware UI insets, or header spacing. Reason: Guarantees zero regression and continuous automated compliance with Arinara design standards across all developer and CI environments. Example violation: Bypassing Gradle verification or disabling automated rule checks.

## Design
### R-004 - Photos Gallery Card Design System Realization
Photo and album cards must strictly reproduce the brand aesthetic established in the visual reference: near-black dark navy screen background (`#0D1117`), dark rounded rectangle cards (`#161C26`, 24dp radius), 1dp subtle structural border (`#262F3E`), electric violet accents (`#7A5AF8`), and clear typographic hierarchy. Reason: Preserves distinctive brand identity, legibility, and visual hierarchy. Example violation: Rendering legacy light cards or using unstyled raw Compose components.

### R-005 - Zero Placeholder Shipped Work
All shipped screens and components must provide complete empty, loading, error, and populated states. No dummy mock data, TODOs, or fake stubs in production source code (`app/src/main/`). Reason: Ensures production-grade readiness at every milestone. Example violation: Leaving `// TODO: implement feature` or hardcoding sample lists in production viewmodels.

### R-010 - Borderless Real-Media Player & Concise Consumer Copy
All UI cards, chips, buttons, bottom sheets, and floating bars must render cleanly without 1dp structural outline borders (`Modifier.border(...)`). The app must index and play only real device media via Android `MediaStore` and `ExoPlayer` with automatic permission prompts on launch—never fabricated sample files, never a manual `"Scan Device"` button, never a `"Cut"` bottom tab, and never `"Studio"` branding or verbose technical descriptions. Reason: Delivers a clean, borderless visual hierarchy and authentic media playback experience. Example violation: Rendering `.border(1.dp, ...)` on cards, generating fake sample files at startup, or displaying `"Studio"` in user-facing text.

## Release
### R-006 - Release Artifact Discipline
Shippable release artifacts must be packaged and copied into `/Output/Release/` adhering to the standard naming template `Photos_<x.y.z>[_Beta][_<Platform>_<Arch>].apk`. Intermediate build outputs in `build/` or `app/build/` must remain gitignored. Reason: Ensures clean, reproducible release tracking. Example violation: Copying unversioned `app-debug.apk` directly to repository root.

### R-007 - Autonomous GitHub Release Workflow
GitHub releases, tags, asset attachments, and repository operations are executed autonomously using pre-configured machine credentials (`GH_TOKEN` / Windows Keyring `gh auth`) via GitHub CLI (`gh`). Releases must bundle shippable artifacts from `/Output/Release/` and notes from `/Changelog/`. Reason: Guarantees reliable, unprompted release deployment across AI sessions and tools. Example violation: Requesting manual user credentials or skipping APK asset upload.

### R-008 - Desktop Workstation Directory Discipline
The desktop widescreen application lives in the dedicated PascalCase root folder `/PhotosDesktop`. Desktop release binaries are packaged into `/Output/Release/Photos_<x.y.z>[_Beta]_Windows_x64.exe`. Reason: Isolates desktop web and native shell code while preserving root PascalCase topic folder discipline. Example violation: Dropping desktop files directly at project root.
