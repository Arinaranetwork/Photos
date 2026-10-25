# Phase 1 - BoilerplateAndArchitecture

## Goal
Establish a robust, production-grade Android project structure, Gradle build configuration, Electric Slate design system, component presets, and automated UI rule enforcement for the Photos application matching Fotara's conventions.

## Scope
- Gradle multi-module project configuration (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, Gradle wrapper).
- Automated `VerifyAndroidRulesTask` in `app/build.gradle.kts` enforcing `Android.md` rules AR-001 through AR-016.
- Android application manifest, package namespace `com.arinara.photos`, Application class (`PhotosApp`), and main activity (`MainActivity`).
- Design system and theme infrastructure (`DesignTokens.kt`, `SpringPhysics.kt`, `Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, `Theme.kt`).
- Complete set of standardized UI component presets in `com.arinara.photos.ui.components.presets` (`AppScreen`, `AppTopBar`, `AppCard`, `AppButton`, `AppFloatingBar`, `AppListItem`, `AppNoticeBanner`, `AppPillChip`, `AppSlider`, `AppBottomSheet`, `AppDropdownMenu`, `ConnectedInspector`).
- Initial functional Home screen and gallery shell complying with safe bottom clearance, hardware UI insets, and zero dummy/fake placeholders.
- Font assets (`elms_sans_bold.ttf`, `elms_sans_medium.ttf`, `elms_sans_light.ttf`), app icons, and legal files (`PRIVACY.md`, `TERMS.md`).
- Automated tests verifying Gradle build and design token adherence.

## Out Of Scope
- Advanced SQLite/Room database schema for persistent media albums (Phase 2).
- Full MediaStore photo indexing pipeline and image viewer canvas (Phase 2).
- Video playback decoder integration (Phase 3).

## Features
### Automated Android UI Rule Enforcement
A custom Gradle task `VerifyAndroidRulesTask` registered to `preBuild`, `check`, and test lifecycles to detect any violation of `Android.md` (hardcoded colors, arbitrary corner radii, improper button usage, missing hardware insets, missing bottom clearance).

### Electric Slate Design System
Centralized theme tokens in `com.arinara.photos.ui.theme.ElectricSlateTokens` and `com.arinara.photos.theme` defining canvas background `#0D1117`, panel `#161C26`, elevated surface `#1F2736`, subtle border `#262F3E`, electric violet primary accent `#7A5AF8`, and muted text `#94A3B8`.

### Universal Spring Physics
Physics-based interaction model via `SpringPhysics` providing `JigglePress` on clickables (`scale(0.90f) -> 1.08f -> 1.00f`), `ViscousGooey` for fluid attachments, and `DismissFade` for popups.

### Centralized Preset Components
Reusable foundation components in `ui.components.presets` preventing ad-hoc Compose widgets and ensuring unified visual rhythm across all screens.

## UI Mockup
```
+------------------------------------------+
| Photos                       [Search] [:]| (AppTopBar, <= 56dp)
+------------------------------------------+
| Timeline                                 |
|                                          |
| [ All ] [ Camera ] [ Screenshots ]       | (AppPillChip row)
|                                          |
| +--------------------------------------+ |
| | [Empty State: No photos indexed yet] | | (AppCard, 24dp radius)
| | Grant media permissions or take photo| |
| +--------------------------------------+ |
|                                          |
|                                          |
|           (>= 100dp clearance)           |
+------------------------------------------+
|      ( [Home]  [Albums]  [Settings] )    | (AppFloatingBar, 999dp)
+------------------------------------------+
```

## Logic Notes
- `PhotosApp` initializes Coil ImageLoader with memory and disk cache configs.
- `MainActivity` enables edge-to-edge drawing and wraps UI in `PhotosTheme` and `AppScreen`.
- `AppScreen` automatically reserves `DefaultBottomClearance` (100.dp) for scrollable contents.

## Risks
- Risk: Gradle version mismatch or missing JDK 17 environment. Mitigation: Standardized toolchain and `gradle.properties` referencing user's verified JDK path.
- Risk: Android.md verification regex false positives. Mitigation: Strict porting of Fotara's proven `VerifyAndroidRulesTask` logic with exact token whitelists.

## Dependencies
- Android Gradle Plugin 9.0.1
- Kotlin 2.3.20 & Jetpack Compose BOM 2026.03.01
- AndroidX Core, Activity Compose, Lifecycle, Material 3, Coil Compose

## Acceptance Criteria
- Complete Gradle build succeeds with zero errors.
- `VerifyAndroidRulesTask` runs cleanly and reports zero violations against `Android.md`.
- All required directories and files exist according to Arinara repository discipline.
- Unit tests pass.
