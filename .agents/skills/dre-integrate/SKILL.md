# dre-integrate

Integrate dre-kt (Dispatch → Reduce → Effects) into an Android/Kotlin project.

## Subcommands

| Subcommand | Description |
|------------|-------------|
| `setup` | Add dre-kt dependency and configure project |
| `feature <name>` | Scaffold a new feature with Contract/Reducer/ViewModel |
| `migrate` | Migrate existing ViewModel from MVI/MVVM to DRE pattern |

## Routing

1. Parse subcommand from arguments (first word)
2. Load `references/{subcommand}-guide.md` (setup → `setup-guide.md`, feature → `feature-scaffold.md`, migrate → `migration-guide.md`)
3. Always load `references/api-reference.md` as context
4. Execute with remaining arguments

## When No Subcommand

Use `AskUserQuestion` to ask:

| Option | Description |
|--------|-------------|
| Setup | First-time integration — add dependency, create base structure |
| New Feature | Scaffold Contract/Reducer/ViewModel for a feature |
| Migrate | Convert existing ViewModel to DRE pattern |

## Core References

- `references/api-reference.md` — DRE types, classes, and usage patterns
- `references/setup-guide.md` — Dependency setup and project configuration
- `references/feature-scaffold.md` — Feature scaffolding workflow
- `references/migration-guide.md` — MVI/MVVM to DRE migration
- `references/examples.md` — Complete code examples
