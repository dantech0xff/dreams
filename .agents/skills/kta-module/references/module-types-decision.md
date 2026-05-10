# Module Type Decision Guide

Three flavors are supported. Pick deliberately. Mismatches cause build cycles, wasted DFM overhead, or shipping code in the base APK that should be deferred.

## At a Glance

| Question | Library | Feature Module | Dynamic Feature |
|---|---|---|---|
| AGP plugin | `com.android.library` | `com.android.library` | `com.android.dynamic-feature` |
| Owns a nav route? | No | Yes | Yes |
| Depends on application module? | No (reverse: app depends on it) | No (registered manually in app deps) | **Yes** (`implementation(project(":app"))` or whatever the app module is named) |
| Delivery | Always in base APK | Always in base APK | install-time / on-demand / conditional / instant |
| Reusable by other modules? | Yes | Discouraged (creates feature→feature coupling) | **No** (DFM cannot be a dep — cycle) |
| Manifest needs `<dist:module>`? | No | No | **Yes** |

## Library Module

### Use For
- Reusable utilities (string helpers, date formatters, ID generators)
- Shared data layer (repositories, DTOs, services)
- Domain logic (use cases, managers)
- Design system primitives (theme tokens, atomic Compose components)
- Cross-cutting infra (DI bases, dispatchers, logging)

### Pros
- Cheapest to build and consume
- Can be depended on by anything (no application reverse-dep)
- Fast incremental builds
- Easy to extract / move / publish externally later

### Cons
- Always ships in base APK — no deferred install
- Cannot own navigation routes that participate in dynamic delivery split graphs

### Anti-patterns
- Owns a full screen + nav route + ViewModel for a user-facing feature → use Feature Module or Dynamic Feature instead
- Module has zero consumers besides one feature → just inline it into that feature

## Feature Module

A regular Android library that hosts a feature (UI + ViewModel + nav) but ships in the base APK.

### Use For
- Feature you want as a module for separation, but you're certain you'll never want on-demand/conditional/instant delivery
- Feature whose API surface is consumed by multiple other modules (DFMs cannot be dependencies)
- Project doesn't use Play Feature Delivery at all

### Pros
- No application reverse-dep — can be a dependency for other modules
- Simpler than DFM (no `<dist:module>` manifest, no SplitInstall plumbing)
- Smaller surface for module-graph mistakes

### Cons
- Always ships in base APK, even if rarely used
- Hard to migrate to DFM later (module graph rewrites, navigation registration changes)
- If the project's convention is DFM-everywhere, this is a divergence — flag it

### Anti-patterns
- "I want this feature on-demand" → must be Dynamic Feature
- "This is a util" → must be plain Library

## Dynamic Feature Module

### Use For
- Feature that may eventually need on-demand install (premium feature, large model bundle, locale-gated content)
- Conditional delivery (feature only for certain SDK / device / country / language)
- Instant app entry points
- Large feature you want to keep out of the base APK
- Codebase already uses DFMs and a new feature should match precedent

### Pros
- Optional deferred install reduces base APK size
- Forced module-graph hygiene: cannot accidentally become a dependency for shared libraries
- First-class support from Play (SplitInstall, conditional delivery, instant)

### Cons
- Reverse-depends on the application module — cannot be consumed by any other module
- Extra wiring (manifest `<dist:module>`, `dynamicFeatures += setOf(...)`, project-specific registration)
- Slightly slower clean builds vs plain library
- More boilerplate per module

### Anti-patterns
- Util / helper code → cycle nightmare; use Library
- Code consumed by other features → DFM cannot be a dep; use Library
- One-off composable with no route → just put it in a shared library or the consuming feature

## Sizing Heuristic

| LOC (rough) | Default |
|---|---|
| < 200, no UI | Inline into consumer; don't make a module |
| 200–2,000, shared | Library |
| 2,000+, owns routes, always-ship | Feature Module |
| Any, on-demand/conditional/instant | Dynamic Feature, no other choice |

## Recovery Paths (If You Picked Wrong)

- **Library → Dynamic Feature**: change plugin, add application reverse-dep, add `<dist:module>` manifest, add to `dynamicFeatures`, register in project's feature graph. Painful but mechanical.
- **Dynamic Feature → Library**: only possible if no other DFM/feature consumes it (they can't). Drop application dep, change plugin, remove from `dynamicFeatures`, deregister.
- **Feature Module → Dynamic Feature**: similar to Library → Dynamic Feature.
- **Feature Module ↔ Library**: cheap if no nav surface; just toggle whether the module owns a route.

Picking right the first time saves 1–2 hours of refactor.

## Project Convention Override

Whatever the existing codebase prefers usually wins, even if the matrix above suggests otherwise. Detect the precedent in Step 0 of the SKILL.md flow and prefer consistency unless the user has a strong reason to diverge.
