# Test Patterns

## Test Class Template

```kotlin

import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.Assert.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import app.cash.turbine.test

class {ClassName}Test {

    private lateinit var sut: {ClassName}
    private val mockDep = mockk<Dependency>(relaxed = true)

    @Before
    fun setUp() {
        clearAllMocks()
        sut = {ClassName}(mockDep)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `{methodName} should {behavior} when {condition}`() = runTest {
        // Arrange
        // Act
        // Assert
    }
}
```

## MockK Patterns

```kotlin
// Relaxed mock (auto-return defaults)
private val mockDep = mockk<Dependency>(relaxed = true)

// Suspend function mocking
coEvery { mockDep.suspendFn(any()) } returns Result.success(value)

// Regular function mocking
every { mockDep.regularFn(any()) } returns value

// Capture arguments
val slot = slot<String>()
coEvery { mockDep.fn(capture(slot)) } returns Unit

// Verification
coVerify(exactly = 1) { mockDep.suspendFn(any()) }
verify(exactly = 0) { mockDep.neverCalledFn() }
```

## Turbine Flow Testing

```kotlin
// Single emission
viewModel.stateFlow.test {
    val state = awaitItem()
    assertEquals(expected, state.field)
    cancelAndIgnoreRemainingEvents()
}

// Multiple emissions
flow.test {
    assertEquals(first, awaitItem())
    assertEquals(second, awaitItem())
    awaitComplete()
}

// Error emission
flow.test {
    val error = awaitError()
    assertTrue(error is ExpectedException)
}
```

## Statistical Testing

```kotlin
// For random behavior verification
repeat(50) {
    val result = randomFunction()
    assertTrue(result in expectedRange)
}
```

## Test Naming

Use backtick descriptive names:
- `{methodName} should {behavior} when {condition}`
- `{methodName} should throw exception when invalid input`
- `{methodName} should emit updated state after action`

## File Location

```
Source: app/src/main/java/com/creative/KotlinAccelerator/biz/{path}/{Class}.kt
Test:   app/src/test/java/com/creative/KotlinAccelerator/biz/{path}/{Class}Test.kt
```

## Run Tests

```bash
./gradlew test --tests "*{TestClassName}*"
```
