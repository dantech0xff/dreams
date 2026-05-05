# Edge Case Discovery

Given method analysis from `analyze_kotlin.py units`, reason through these categories:

## 1. Input Boundaries

For each parameter:
- Empty/null: `""`, `null`, `emptyList()`, `emptyMap()`
- Boundary: `0`, `-1`, `Int.MAX_VALUE`, `Long.MIN_VALUE`
- Invalid format: malformed UUID, invalid email, special characters

## 2. State Transitions

For stateful classes:
- Initial state (before any method call)
- Already in target state (idempotency)
- Concurrent state changes (multiple coroutines)
- Invalid state transitions

## 3. Dependency Failures

For each dependency in `dependencies[]`:
- Returns `null` or empty
- Throws exception
- Returns `Result.failure()`
- Times out (suspend functions)
- Network unavailable

## 4. Coroutine Scenarios

For `suspend` methods:
- Cancellation mid-execution
- Multiple simultaneous calls
- Race conditions on shared state
- Dispatcher switching

## 5. Flow Emissions

For Flow-returning methods:
- Empty flow (no emissions)
- Single emission
- Multiple rapid emissions
- Error during emission
- Collector cancellation

## Analysis Template

```markdown
## Edge Cases: {ClassName}.{methodName}

**Signature**: `{visibility} {suspend?} fun {name}({params}): {returnType}`

### Input Boundaries
- param1: [specific boundary cases]

### State Dependencies
- [what state must exist before calling?]

### Dependency Failures
- {depName}: [what happens if fails?]

### Recommended Tests
1. `{methodName} should {behavior} when {condition}`
2. `{methodName} should handle {edgeCase}`
```

## Project-Specific Patterns

From existing tests in this project:

### SyncManager edge cases
- User not authenticated → return early
- Remote profile null → use local + auth data
- Already syncing → prevent concurrent sync

### LifelineHelper edge cases
- Already used → throw/return error
- Invalid question state → handle gracefully
- Randomness verification → statistical tests (50+ iterations)

### UserManager edge cases
- Zero experience → level stays 1
- Max level reached → no overflow
- Rapid profile updates → latest wins
