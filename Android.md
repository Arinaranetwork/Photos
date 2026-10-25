# Android - Photos
Skills: ArinaraInternal, Android
Last Updated: 2026-10-24

## Theme
Dominant background: Dark (Electric Slate)

## Palette
Strict Electric Slate dark theme tokens. Max 3 active semantic colors per screen (ArinaraInternal I-20). Tints, shades, and opacities count as their parent color.
| Token          | Hex       | Role    | Used for                                                              |
|----------------|-----------|---------|-----------------------------------------------------------------------|
| BgCanvas       | #0D1117   | Base    | Main screen background canvas and dot-grid background                 |
| SurfacePanel   | #161C26   | Base    | Primary cards, bottom docks, settings panels, large content modules   |
| SurfaceElevated| #1F2736   | Base    | Dropdowns, floating overlays, tooltips, connected inspector, list rows|
| BorderSubtle   | #262F3E   | Base    | 1dp subtle structural outlines and dividers                          |
| TrackInactive  | #283245   | Base    | Inactive slider tracks, neutral containers, secondary buttons         |
| TextPrimary    | #F8FAFC   | Neutral | High-contrast primary text, headings, prominent timestamps            |
| TextMuted      | #94A3B8   | Neutral | Secondary labels, descriptions, subtitles, inactive icons             |
| PrimaryAccent  | #7A5AF8   | Primary | Electric violet: active states, slider thumbs, droplet neck, FABs     |
| Destructive    | #DC5A5A   | Alert   | Destructive actions, delete buttons, error banners, critical warnings |

Dark-dominant: Every surface fill at HSL lightness <= 22%. Primary text at lightness >= 95%.
Max Color Discipline: Exactly 1 primary accent (#7A5AF8) across all active elements on screen. No multi-colored tool interfaces.

## Typography
Family: ElmsSans (Primary UI), FontFamily.Monospace (Numeric timers, coordinates, clock faces)
Weight hierarchy: Title >= Subtitle >= Content (I-04). Sizes in sp.
| Role     | Size | Weight   | Line height | Color       | Position                         |
|----------|------|----------|-------------|-------------|----------------------------------|
| Title    | 22   | Bold     | 28          | #F8FAFC     | Same container anchor on every screen |
| Subtitle | 16   | Medium   | 22          | #94A3B8     | Same start edge on every screen  |
| Content  | 14   | Regular  | 20          | #94A3B8     | Same start edge on every screen  |
Optional roles: Caption: size 12, weight Light, line height 16, color #94A3B8 | Label: size 11, weight Medium, line height 14, color #94A3B8

## Shape
Standardized Preset Shape Hierarchy. Never create ad-hoc or arbitrary `RoundedCornerShape` values in screens.
| Shape Token | Radius (dp) | Used for                                                                 |
|-------------|-------------|--------------------------------------------------------------------------|
| Capsule     | 999         | Floating docks (`AppFloatingBar`), chips (`AppPillChip`), floating pills|
| Sheet       | 28 (top)    | Modal sheets (`AppBottomSheet`), expanded draw-up panels                 |
| Card        | 24          | Primary cards (`AppCard`), canvas inspector panels, major feature modules|
| Panel       | 20 / 16     | Contextual overlays, dialogs, notice banners (`AppNoticeBanner`)        |
| Row         | 12 / 14     | List rows (`AppListItem`), input fields, secondary action containers     |
| Circle      | 50%         | Floating Action Buttons (`AppFAB`), circular icon buttons, status badges |

## Spacing & Bottom Clearance
Tokens: 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 100 (dp).
| Use                          | Value |
|------------------------------|-------|
| Screen start and end padding | 16    |
| Container padding            | 16 standard, 20 comfortable, 24 large panels |
| Related elements             | 8     |
| Label to control             | 12    |
| Between components           | 16    |
| Group to group               | 16    |
| Section to section           | 24-32 |
| Major section                | 32-48 |
| Safe bottom page clearance   | >= 100 dp (mandatory via `AppScreen`, `contentPadding`, or `BottomClearanceSpacer`)|

## Universal Spring Physics
Zero rigid linear motion. All touch feedback, gestures, and transitions must resolve through `SpringPhysics`:
- **JigglePress**: `spring(dampingRatio = 0.5f, stiffness = 180f)` on interactive clickables via `Modifier.jigglePressClickable` (scale 0.90f on touch down, 1.08f overshoot on release, settles at 1.00f).
- **ViscousGooey**: `spring(dampingRatio = 0.45f, stiffness = 100f)` for organic slider stretches, droplet inspector necks, and fluid expansions.
- **DismissFade**: `spring(dampingRatio = 0.6f, stiffness = 300f)` for pop-up scale exit and alpha collapse.

## Icons
Set: Outlined Material Symbols & Material Icons
Style: Outlined
Stroke: 2dp uniform
Sizes (dp): 16, 18, 20, 24, 36 (Primary FAB/Action)

## Navigation Map
Persistent navigation: Bottom floating dock / navigation bar: Home, Albums, Settings
System back: Exits app on Home root, clears selection mode or pops backstack elsewhere.
| Screen     | Entry point                | Back or Home control             |
|------------|----------------------------|----------------------------------|
| Home       | Persistent Bottom Bar Home | none (persistent navigation covers it) |
| Albums     | Persistent Bottom Bar Albums| none (persistent navigation covers it) |
| Settings   | Persistent Bottom Bar Settings | none (persistent navigation covers it) |

## Component Presets
All screens and views must be assembled exclusively from the 10 centralized presets in `ui/components/presets/`:
1. `AppTopBar`: Standard header with rotation jiggle back navigation and trailing actions.
2. `AppCard`: 24dp rounded panel with `SurfacePanel`, 1dp `BorderSubtle`, and optional ambient glow.
3. `AppFloatingBar`: 999dp floating pill dock with spring slide-up entrance.
4. `ConnectedInspector`: Contextual tool settings attached via `ViscousGooey` droplet neck.
5. `AppSlider`: Stepped discrete dot indicators, tactile touch scale pop (1.0f -> 1.25f), and magnetic snap bounce.
6. `AppBottomSheet`: 28dp top corners, elastic gesture resistance, minimizable into bottom floating pill.
7. `AppListItem`: Standardized row with icon box, title/subtitle, and Chevron, Bouncy Switch, or Shaking Trash actions.
8. `AppButton` & `AppFAB`: Primary, Secondary, Destructive, and circular FAB variants with scale-pop press physics.
9. `AppPillChip`: 999dp toggleable chip with spring scale-pop selection feedback.
10. `AppNoticeBanner`: Info, Warning, Error banner with downward overshoot enter and `DismissFade` exit.
11. `AppScreen`: Scaffold preset with automatic safe bottom clearance (>= 100dp).

## Local Rules
### AR-001 - Strict Token Adherence
All UI elements must resolve dimensions, colors, and line heights from tokens. No ad-hoc hardcoded values. Reason: Visual consistency across density scales. Example violation: Using `Color(0xFF2563EB)` or `fontSize = 38.sp` without matching tokens.

### AR-002 - Stable PointerInput Keys
Pointer input modifiers must never pass mutable gesture offsets or scales as modifier keys, preventing coroutine restart stutter. Reason: Key changes cancel active gestures. Example violation: `.pointerInput(userScale, panOffset)`.

### AR-003 - Mandatory Component Presets
All screens and views must be composed exclusively from the standardized component presets in `ui/components/presets/`. Ad-hoc, raw Jetpack Compose components (`Button`, `Card`, custom `Box` containers with one-off styling) are strictly forbidden. Reason: Prevents fragmented styling and guarantees universal spring physics and design token binding across the entire application. Example violation: Creating a raw `androidx.compose.material3.Button` instead of `AppButton`.

### AR-004 - Standardized Shape Hierarchy
Never create ad-hoc or arbitrary `RoundedCornerShape` values in screen composables. All corner radii must strictly resolve to standard shape tokens: `999.dp` (Capsule/Pill), `28.dp` (Bottom Sheet), `24.dp` (Card), `20.dp` / `16.dp` (Inspector/Panel), `12.dp` / `14.dp` (Row/Input), or `CircleShape` (FAB/Control). Reason: Inconsistent corner radii break visual rhythm and brand coherence. Example violation: Using `RoundedCornerShape(7.dp)` or `RoundedCornerShape(35.dp)`.

### AR-005 - Max 3 Active Colors Per Screen
A single screen view must never display more than 3 active semantic colors simultaneously: Neutral Base/Surface hierarchy, Electric Violet Primary Accent (`#7A5AF8`), and Destructive Alert (`#DC5A5A`) when handling critical warnings or deletions. Prohibit multi-colored or rainbow toolbars and buttons. Reason: Clashing colors degrade legibility and violate Arinara corporate brand guidelines. Example violation: Displaying cyan, amber, royal blue, and cream buttons within the same screen.

### AR-006 - Safe Bottom Page Clearance
Every screen and scrollable container must reserve a minimum of `100.dp` safe clearance at the bottom of the page (`AppScreen`, `contentPadding = PaddingValues(bottom = 100.dp)`, or `BottomClearanceSpacer()`). Reason: Prevents floating bottom navigation bars and tool docks from occluding buttons, text fields, or list items at the bottom of the screen. Example violation: A `LazyColumn` without bottom padding where the last two items are hidden behind the floating bar.

### AR-007 - Centralized Muted Tone Discipline
Secondary text, descriptions, subtitles, inactive slider tracks, switch tracks, and structural dividers must strictly resolve to `TextMuted` (`#94A3B8`), `BorderSubtle` (`#262F3E`), or `TrackInactive` (`#283245`). Predefined preset defaults encapsulate these contracts and must not be overridden with custom low-contrast or high-contrast values. Reason: Maintains clear visual hierarchy without creating competing high-contrast elements. Example violation: Using semi-transparent white or random gray hex values for subtitle labels.

### AR-008 - Standardized Outlines and Photo Borders
All element borders, image frames, and photo outlines must strictly use `1.dp` (`BorderSubtle`) for structural definition or `2.dp` (`PrimaryAccent`) for active focus/selection rings. Arbitrary border widths (e.g., `3.dp`, `4.dp`) are forbidden. Avatars must be clipped with `CircleShape`; card and photo containers must strictly use standard radii (`16.dp`, `20.dp`, `24.dp`). Reason: Inconsistent outlines and borders fragment image cards and profile avatars. Example violation: Using `.border(3.dp, Color.White)` around an image or thumbnail.

### AR-009 - Mandatory Modal and Popup Presets
Screens must never instantiate raw Compose `AlertDialog`, unstyled `Dialog`, or raw `ModalBottomSheet`. All secondary interactions, dialogs, and popups must be encapsulated in `AppBottomSheet` (with `28.dp` top corners and minimize-to-pill interaction) or `ConnectedInspector`. Reason: Unstyled popups violate the Electric Slate dark theme and lack universal spring physics. Example violation: Invoking `androidx.compose.material3.AlertDialog` directly inside a screen.

### AR-010 - Standardized Typography Hierarchy
Hardcoded `fontSize = X.sp` with arbitrary numbers is strictly prohibited. Font sizes must resolve to standard typography scale tokens: Title (`22.sp`), Subtitle (`16.sp`), Content (`14.sp`), Caption (`12.sp`), or Label (`11.sp`). Reason: Prevents fragmented font hierarchies across screens and respects system density scaling. Example violation: Using `fontSize = 17.sp` or `fontSize = 35.sp`.

### AR-011 - Strict Surface Elevation & Shadow Discipline
Raw drop shadow modifiers (`.shadow(...)`, `.dropShadow(...)`) are prohibited. Visual depth must be established through Material 3 tonal surface elevation (`surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`) paired with `1.dp` `BorderSubtle`. Reason: Dirty drop shadows degrade dark theme aesthetics and contrast clarity. Example violation: Adding `.shadow(8.dp, RoundedCornerShape(16.dp))` to a card container.

### AR-012 - Prohibit White Component Borders
Component borders must never use `Color.White`, `0xFFFFFFFF`, `#FFFFFF`, or white alpha variants (`Color.White.copy(...)`). All borders across UI components and editors (including canvas drawing editors, swatches, and photo selectors) must strictly resolve to `BorderSubtle` (`#262F3E`) for structural outlines or `PrimaryAccent` (`#7A5AF8`) for active focus/selection rings. Reason: Harsh pure white borders break Electric Slate dark mode contrast harmony and brand consistency. Example violation: `.border(2.dp, Color.White, CircleShape)` or `color = if (isSelected) Color.White else BorderSubtle`.

### AR-013 - Single Border Color Per Screen
A single screen view must use at most one unified border color token across all of its component borders (e.g. `BorderSubtle` for structural borders, with `PrimaryAccent` reserved solely for the active selection state). Screens must never mix multiple ad-hoc or competing border colors. Reason: Consistent border hues maintain structural calm and eliminate distracting visual noise. Example violation: A screen combining cyan borders, amber outlines, and dark gray dividers.

### AR-014 - Settings Selection Rectangle Aspect Ratio and Uniform Color
In settings screens and dialogs (e.g. `SettingsScreen`, `BorderPickerDialog`), all selection rectangles and option cards must enforce equal width and height (`Modifier.aspectRatio(1f)`) and resolve to uniform token colors (`SurfacePanel`, `BorderSubtle`, and `PrimaryAccent` for active selection). Reason: Inconsistent card proportions and colors in settings grids look unaligned and disorganized. Example violation: A rectangular selection card in settings grid without `aspectRatio(1f)`.

### AR-015 - Hardware UI Clearance & Overflow Prevention
All screens must position their contents strictly above hardware UI boundaries: the top status bar (battery, time, notification icons) and the bottom system navigation bar. Content overflow behind the hardware UI is prohibited. Screens must use `AppScreen`, `statusBarsPadding()`, `navigationBarsPadding()`, or `WindowInsets.statusBars` / `WindowInsets.navigationBars`. Reason: Content drawn under system status or navigation bars is illegible and prone to accidental touches. Example violation: A screen container lacking status bar padding where header text is clipped by camera cutouts or status icons.

### AR-016 - Compact Top Layer Spacing
The top layer and heading section of screens must maintain compact vertical spacing without excessive vertical gaps. Spacers at the top level or within top bars must not exceed 24.dp (no `Spacer(modifier = Modifier.height(X.dp))` where X > 24dp at the top layer), and header heights must not exceed 56.dp (`AppTopBar`). Reason: Large top gaps waste critical screen real estate on mobile devices and push important content off-screen. Example violation: Using `Spacer(modifier = Modifier.height(60.dp))` at the top of a screen column.
