# Composable Splitting Checks

Decide when to extract sub-composables. Goal: smaller recompose scope, clearer concerns, testable previews.

## When to split

| Trigger | Action |
|---------|--------|
| Function > 80 LOC | Split into 2–3 sub-composables |
| 3+ visual sections (header / body / footer) | Each section → its own `@Composable` |
| Mixed concerns (state hoisting + layout + side effects) | Hoist state outward; pure stateless leaf |
| Repeated sub-layout (`Row { Icon; Text }` x4) | Extract into `private @Composable fun ItemRow(...)` |
| Hot recomposition path mixed with cold UI | Isolate hot leaf so cold parents stay cached |
| Stateful + stateless variants needed for previews | Stateful wrapper + stateless content pair |

## Patterns

### 1. Section split (most common)

Before:
```kotlin
@Composable fun ProfileScreen(state: UiState, onAction: (A) -> Unit) {
    Column {
        // 25 LOC header
        // 40 LOC content list
        // 20 LOC footer with CTAs
    }
}
```

After:
```kotlin
@Composable fun ProfileScreen(state: UiState, onAction: (A) -> Unit) {
    Column {
        ProfileHeader(state.user)
        ProfileContent(state.items, onItemClick = { onAction(A.Click(it)) })
        ProfileFooter(canEdit = state.canEdit, onEdit = { onAction(A.Edit) })
    }
}

@Composable private fun ProfileHeader(user: User) { ... }
@Composable private fun ProfileContent(items: ImmutableList<Item>, onItemClick: (Id) -> Unit) { ... }
@Composable private fun ProfileFooter(canEdit: Boolean, onEdit: () -> Unit) { ... }
```

Each child reads the smallest slice → parent recompose doesn't drag children.

### 2. Stateful / stateless pair

Use when a Composable manages local state but a preview / test needs a stateless variant.

```kotlin
@Composable fun ExpandablePanel(
    title: String,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExpandablePanelContent(
        title = title,
        expanded = expanded,
        onToggle = { expanded = !expanded },
        content = content,
    )
}

@Composable private fun ExpandablePanelContent(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) { ... }
```

Preview targets the stateless `Content`.

### 3. Hot-path isolation

When one piece (e.g. timer) updates frequently and the rest is static:

```kotlin
@Composable fun GameHeader(question: Question, secondsLeft: Int) {
    QuestionTitle(question.text)        // cold, stable
    Timer(secondsLeft)                  // hot — isolated, only this recomposes
    AnswerCount(question.answers.size)  // cold, stable
}
```

Without isolation, every `secondsLeft` tick recomposes title + count too.

### 4. List item extraction

Required for `key`-based memoization and `animateItem` to work cleanly.

```kotlin
LazyColumn {
    items(list, key = { it.id }) { item -> LeaderboardRow(item) }
}

@Composable private fun LeaderboardRow(item: LeaderboardEntry) { ... }
```

### 5. Slot API instead of split

If parent passes lots of layout-shaped data, prefer slot lambdas:

```kotlin
@Composable fun Card(
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
    footer: @Composable () -> Unit = {},
) { Column { header(); body(); footer() } }
```

## Anti-patterns (don't split)

- **Trivial leaf** (`Text(label)`): no benefit, hurts readability.
- **Wrapper that adds nothing**: passing all params straight through is dead weight.
- **Splitting across files** when no other caller exists: keep `private` in same file.
- **Premature generic component**: extract on second use, not first (rule of two).
- **Splitting just to reduce LOC**: only split if it improves recompose scope or readability.

## Naming

- Match project prefix: `Qzds*` for shared design system, plain PascalCase + `private` for file-local helpers.
- Suffix conventions: `Section`, `Row`, `Header`, `Footer`, `Card`, `Item`, `Content` (paired with stateful wrapper).
- File-local helpers stay `private` in the same `.kt`.

## Output format for findings

```
[split][medium] L120–L185 (65 LOC): two distinct sections
  Suggestion: extract `ProfileFooter(canEdit, onEdit)` as private @Composable in same file
  Benefit: footer recompose isolated from list updates
```
