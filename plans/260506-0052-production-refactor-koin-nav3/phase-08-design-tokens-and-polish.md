# Phase 08 — Design Tokens + Polish

## 1. Context Links
- Parent: [plan.md](plan.md)
- Depends on: [phase-06-shared-element-and-motion.md](phase-06-shared-element-and-motion.md)
- Inputs: scout §10 (theme files), `research/researcher-02-nav3-shared-elements.md` §5 (motion accessibility)
- Docs: https://m3.material.io/styles/color/system/overview | https://m3.material.io/styles/typography/overview

## 2. Overview
- **Date:** 2026-05-06
- **Description:** Light track. Extract semantic design tokens from hardcoded M3 palette. Add a minimal Settings sheet to surface reduced-motion toggle (phase-05 backend). Audit dark theme color contrast on all screens.
- **Priority:** P3
- **Implementation status:** pending
- **Review status:** pending

## 3. Key Insights
- Existing `Color.kt` (scout §10) defines six raw color values (`Purple80/40`, `PurpleGrey80/40`, `Pink80/40`). They map directly to `MaterialTheme.colorScheme.primary/secondary/tertiary` already.
- "Tokens" here means *semantic aliases* used by feature code (e.g. `Tokens.surfaceVariantElevated`) when M3 doesn't have a sufficient slot. KISS: only introduce a token when feature code currently hardcodes a color (`Color(0xCCFFFFFF)` in `LandingScreen.kt:55` is one such case).
- Typography: only `bodyLarge` is customized (scout §10). M3 default scale is fine for an educational app — don't over-engineer. YAGNI.
- Settings UI: simplest = a `ModalBottomSheet` reachable from `LandingScreen` "About" → settings cog OR a new top-bar action in `GalleryScreen`. Pick **gallery top-bar action** for discoverability.
- Reduced-motion toggle is the only setting today. Avoid ceremony: one switch.

## 4. Requirements

### Functional
- `Tokens.kt` exposes: `Tokens.translucentSurfaceLight = Color(0xCCFFFFFF)` and any other ad-hoc colors found via `grep "Color(0x" app/src/main/java/com/dantech/dreams/ui`. Replace inline literals.
- `SettingsSheet` composable: bottom sheet with reduced-motion `Switch`. Read+write through `UserPrefsRepository`.
- Gallery top-bar gains gear icon → opens settings sheet.

### Non-Functional
- No new file > 200 lines.
- No new color literals in feature code post-phase.
- Dark theme audit: text contrast ≥ AA on all screens.

## 5. Architecture

```
ui/theme/
├── Color.kt        ← raw palette (existing)
├── Type.kt         ← typography (existing)
├── Theme.kt        ← DreamsTheme M3 wiring (existing)
└── Tokens.kt       ← NEW: semantic aliases for non-M3-slot colors

ui/feature/settings/
└── SettingsSheet.kt  ← NEW: ModalBottomSheet + reduced-motion switch
                       reads userPrefs.reducedMotionOverride
                       on toggle: vm.setReducedMotion(it)
```

`SettingsViewModel` is a tiny VM around `UserPrefsRepository.setReducedMotion()`; OR skip the VM and call repo directly via `koinInject()` (KISS — no derived state needed). **Decision:** no VM; sheet is a leaf composable.

## 6. Related Code Files

### Modify
- `ui/theme/Color.kt` — keep palette, add NO logic
- `ui/feature/landing/LandingScreen.kt:55` — replace `Color(0xCCFFFFFF)` with `Tokens.translucentLightOnDark`
- `ui/feature/gallery/GalleryScreen.kt` — add gear `IconButton` in TopAppBar `actions`, opening `SettingsSheet`
- `ui/feature/showcase/ShowcaseScreen.kt:38, 77` — `Color.White` literals OK (semantic on dark backdrop) but document why
- `ui/feature/lesson/LessonDetailScreen.kt` — review for any literal Colors

### Create
- `ui/theme/Tokens.kt` — semantic color/spacing/elevation aliases
- `ui/feature/settings/SettingsSheet.kt` — ModalBottomSheet w/ reduced-motion switch

### Delete
- None

## 7. Implementation Steps

1. **Audit color literals:**
   ```bash
   grep -rn "Color(0x" app/src/main/java/com/dantech/dreams/ui/feature
   ```
   List each occurrence. For each: either move to `Tokens` (if reused) or `MaterialTheme.colorScheme.X` (if M3 covers it).

2. **Create `ui/theme/Tokens.kt`:**
   ```kotlin
   object Tokens {
       // Translucent overlays (used over dynamic shader backgrounds where colorScheme would clash).
       val translucentLightOnDark = Color(0xCCFFFFFF)
       val translucentDarkOnLight = Color(0xCC000000)
       // Spacing — only if multiple feature files share magic dp values; otherwise YAGNI
       val spaceCompact = 8.dp
       val spaceComfortable = 16.dp
   }
   ```
   Keep file ≤30 lines. Add tokens only as the audit demands.

3. **Replace literals:** `LandingScreen.kt:55` → `Tokens.translucentLightOnDark`. Check other files; replace where reused.

4. **Create `ui/feature/settings/SettingsSheet.kt`:**
   ```kotlin
   @OptIn(ExperimentalMaterial3Api::class)
   @Composable
   fun SettingsSheet(onDismiss: () -> Unit, prefsRepo: UserPrefsRepository = koinInject()) {
       val prefs by prefsRepo.prefsFlow.collectAsStateWithLifecycle(initialValue = UserPrefs.DEFAULT)
       val scope = rememberCoroutineScope()
       ModalBottomSheet(onDismissRequest = onDismiss) {
           Column(Modifier.padding(24.dp)) {
               Text("Settings", style = MaterialTheme.typography.titleLarge)
               Spacer(Modifier.height(16.dp))
               Row(verticalAlignment = Alignment.CenterVertically) {
                   Column(Modifier.weight(1f)) {
                       Text("Reduce motion", style = MaterialTheme.typography.titleMedium)
                       Text("Skip transitions, render lessons instantly", style = MaterialTheme.typography.bodySmall)
                   }
                   Switch(
                       checked = prefs.reducedMotionOverride,
                       onCheckedChange = { v -> scope.launch { prefsRepo.setReducedMotion(v) } }
                   )
               }
           }
       }
   }
   ```

5. **Edit `GalleryScreen.kt` TopAppBar:**
   ```kotlin
   var settingsOpen by remember { mutableStateOf(false) }  // composable-local OK
   TopAppBar(
       title = { Text("AGSL Playground") },
       actions = {
           IconButton(onClick = { settingsOpen = true }) {
               Icon(Icons.Outlined.Settings, contentDescription = "Settings")
           }
       }
   )
   if (settingsOpen) SettingsSheet(onDismiss = { settingsOpen = false })
   ```

6. **Dark mode pass:**
   - Force dark via `adb shell cmd uimode night yes`.
   - Walk Landing → Gallery → Detail (each category) → Showcase (each).
   - Note any low-contrast text. Fix by switching to `MaterialTheme.colorScheme.onSurface` etc. as needed.
   - Verify `Tokens.translucentLightOnDark` over shader backdrop reads on both themes (shader is dark-ish always, so `translucentLightOnDark` is fine).
   - Restore: `adb shell cmd uimode night auto`.

7. **Build + final regression:** `./gradlew :app:installDebug && ./gradlew test connectedDebugAndroidTest`. Manual: open Settings → toggle reduced motion → navigate gallery→detail → verify instant scene swap.

## 8. Todo
- [ ] Audit + remove inline `Color(0x...)` literals from feature code
- [ ] `Tokens.kt` created with only tokens that earned their place
- [ ] `SettingsSheet.kt` reaches reduced-motion via `UserPrefsRepository`
- [ ] Gallery top-bar gear icon opens sheet
- [ ] Dark-mode contrast walked (notes attached)
- [ ] All tests still green

## 9. Success Criteria
- `grep "Color(0x" app/src/main/java/com/dantech/dreams/ui/feature` returns only justified one-off uses (`Color.White` over dark shader backdrop is OK; document inline).
- Settings sheet → toggle → next nav uses instant transition (verifies phase-05 + phase-06 + phase-08 wiring end-to-end).
- Dark mode: no AA contrast violations on text.

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Tokens file grows into a junk drawer of magic numbers | Med | Low | Hard rule: only add a token if it appears 2+ times. Document in the file header |
| Settings sheet collects scope creep (theme, accent, etc.) | Med | Low | YAGNI — only reduced-motion ships this phase. Open follow-up issues for any other ask |
| `ModalBottomSheet` API behavior on tablet/landscape | Low | Low | M3 default handles both; manual test in landscape |
| Replacing `Color.White` in showcase with theme colors breaks visibility on shader backdrop | Med | Med | Showcase backdrops are always shader gradients (mostly dark); `Color.White` is intentional. Do NOT replace — document in code comment |

## 11. Security Considerations
- None. Settings sheet writes to existing DataStore single (security covered in phase-05).

## 12. Next Steps
- Plan complete. Optional follow-ups (post-merge):
  - Extract dark-mode color contrast tweaks into a real design-system PR
  - GitHub Actions CI for `./gradlew test connectedDebugAndroidTest`
  - Deep-link parity for `Route.LessonDetail(id)` if external link sharing requested

## Unresolved Questions
- **Token vs colorScheme override:** for the translucent overlays, would adding `surfaceContainerHighestVariant` to the M3 colorScheme be cleaner than a Token? Decide based on audit count — if only 1–2 sites, Token wins (simpler).
- **Settings UI placement:** gallery top bar vs landing menu vs Activity-level FAB. Locked to gallery top bar per §3 reasoning; revisit if user-test feedback says otherwise.
