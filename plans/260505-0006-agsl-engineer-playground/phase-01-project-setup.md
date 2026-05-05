# Phase 01 — Project Setup & Dependencies

## Context Links
- `plan.md`
- `reports/researcher-260505-0001-agsl-compose-integration.md` (minSdk requirement)

## Overview
- **Priority:** P1 (blocker for all other phases)
- **Status:** pending
- **Effort:** 2h
- Bump SDK floor for AGSL, add navigation + image deps, lay out package structure, write project README.

## Key Insights
- `RuntimeShader` is API 33+; bumping `minSdk` removes all runtime gating.
- `Navigation-Compose` is the lightest router for a 25-screen app — no Hilt needed.
- Coil only used by Category D demos (image post-FX) — still cheap to add up front.
- `kotlinx-immutable` keeps `LessonRegistry` lists `@Immutable` for stable Compose recomposition.

## Requirements
- App compiles on API 33+ devices and `targetSdk 36`.
- All new modules referenceable from `MainActivity` without unresolved imports.
- README describes purpose, audience, and how to run.

## Architecture
Single-module app. Package layout under `com.dantech.dreams`:
```
ui/
  playground/
    gallery/
    lesson/
    common/
  theme/
shaders/
  basics/
  sdf/
  noise/
  posteffect/
  showcase/
data/
  lesson/
```
No DI; `LessonRegistry` is a top-level `object`.

## Related Code Files
**Modify:**
- `app/build.gradle.kts` — bump `minSdk`, add nav/coil/immutable deps
- `gradle/libs.versions.toml` — add version refs + library aliases
- `app/src/main/java/com/dantech/dreams/MainActivity.kt` — placeholder navhost wiring (real wiring in phase 02)
- `README.md` (create at repo root if missing)

**Create:** empty `.gitkeep`-style package directories listed above (or create via first source file in phase 02).

## Implementation Steps
1. Edit `app/build.gradle.kts`: change `minSdk = 26` → `minSdk = 33`.
2. In `gradle/libs.versions.toml`, add under `[versions]`:
   - `navigationCompose = "2.8.5"` (or latest stable matching BOM)
   - `coil = "2.7.0"`
   - `kotlinxCollectionsImmutable = "0.3.8"`
3. Add `[libraries]` aliases: `androidx-navigation-compose`, `coil-compose`, `kotlinx-collections-immutable`.
4. Add the three `implementation(libs...)` lines to `app/build.gradle.kts`.
5. Sync Gradle; run `./gradlew :app:assembleDebug` to confirm clean build.
6. Create empty package directories (Kotlin requires at least one file per package, defer until phase 02).
7. Write `README.md`: project intent (AGSL learning playground), audience (engineers), minSdk note, build cmd, screenshot placeholder.

## Todo List
- [ ] Bump `minSdk` to 33
- [ ] Add nav-compose / coil / kotlinx-immutable to version catalog
- [ ] Add deps to `app/build.gradle.kts`
- [ ] `./gradlew assembleDebug` passes
- [ ] Create root `README.md`

## Success Criteria
- `assembleDebug` returns 0
- `minSdk` reads `33` in merged manifest
- README explains how to run

## Risk Assessment
| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| BOM `2026.02.01` missing nav-compose stable | Low | Med | Pin nav version explicitly (not BOM-controlled anyway) |
| Coil 2.x vs 3.x API drift | Low | Low | Pick 2.7.x, defer Coil 3 migration |

## Security Considerations
None — no network, no permissions added.

## Next Steps
Phase 02 builds the lesson framework on top of this skeleton.
