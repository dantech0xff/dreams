# Architecture

Framework-agnostic principles for layering, dependencies, and abstraction. All findings here are Major unless noted.

## Dependency direction

Dependencies must flow toward the more stable, more abstract parts of the system. Code in a high-level layer never names a concrete type from a low-level layer.

```
Presentation  ──▶  Application / Use cases  ──▶  Domain  ◀── Infrastructure
```

```kotlin
// BAD: domain depends on infrastructure
package domain
class OrderService(private val repo: PostgresOrderRepository) { … }

// GOOD: domain owns the abstraction; infra implements it
package domain
interface OrderRepository { fun find(id: OrderId): Order? }
class OrderService(private val repo: OrderRepository) { … }

package infrastructure
class PostgresOrderRepository(private val db: Database) : OrderRepository { … }
```

Flag any `import` from a lower layer to a higher layer (presentation → domain is fine; domain → presentation is not).

## Cohesion and coupling

A class should have one reason to change. Two unrelated reasons living in the same class is **low cohesion** — split it. A class that pulls in five unrelated dependencies in its constructor is **high coupling** — extract collaborators or rethink the boundary.

```kotlin
// BAD: low cohesion (mixes parsing, validation, persistence, notification)
class UserManager(
    private val parser: JsonParser,
    private val validator: Validator,
    private val db: Database,
    private val mailer: Mailer,
    private val logger: Logger,
    private val cache: Cache
)

// GOOD: separate use cases
class RegisterUser(private val users: UserRepository, private val mailer: Mailer)
class UpdateUserProfile(private val users: UserRepository)
class SearchUsers(private val users: UserRepository, private val cache: Cache)
```
A constructor with 6+ collaborators is a smell; ask whether the class is doing more than one job.

## SOLID, applied

### Single Responsibility
Each module / class has one well-named axis of change. Names like `Manager`, `Helper`, `Util`, `Service` (without further qualification) are usually a hint of weak responsibility.

### Open / Closed
New variants should not require editing closed code paths. In Kotlin this typically means **sealed hierarchies** + exhaustive `when`, not `if/else` chains on `is`.

```kotlin
// BAD: new payment type requires editing the chain in N places
fun fee(p: Payment): Money = when {
    p is Card  -> ...
    p is Wire  -> ...
    p is Cash  -> ...
    else       -> error("unknown")
}

// GOOD: variants own their behaviour
sealed interface Payment { fun fee(): Money }
data class Card(...) : Payment { override fun fee() = ... }
```

### Liskov
Subtypes must honour the supertype contract: same preconditions or weaker, same postconditions or stronger, no surprise exceptions. A subclass that throws on a method the parent declared total is a Major violation.

### Interface Segregation
Don't force callers to depend on methods they don't use. Several small interfaces beat one fat one.

```kotlin
// BAD
interface Storage {
    fun read(k: String): ByteArray
    fun write(k: String, v: ByteArray)
    fun stream(k: String): InputStream
    fun list(prefix: String): List<String>
    fun delete(k: String)
}

// GOOD
interface Reader { fun read(k: String): ByteArray }
interface Writer { fun write(k: String, v: ByteArray) }
```

### Dependency Inversion
Depend on abstractions you own, not on third-party concretes. The third-party type appears only in the implementation.

## Constructor injection over field / setter / service-locator

```kotlin
// BAD: hidden dependency, untestable
class OrderService { private val repo = ServiceLocator.get<OrderRepository>() }

// BAD: setter injection, mutable
class OrderService { lateinit var repo: OrderRepository }

// GOOD
class OrderService(private val repo: OrderRepository)
```
Constructor injection makes dependencies explicit, makes testing trivial, and forbids partially-constructed objects. Flag `lateinit` used to backfill required collaborators.

## Module / package boundaries

- Packages exist to encode a boundary. A class with `internal` visibility from one package used widely by another is a leak — make it `public` deliberately or move it.
- Cyclic dependencies between packages are Critical when introduced; flag and require breaking the cycle (usually by extracting an interface to a third package).
- A `:feature-a` module reaching into `:feature-b` directly is a layering violation; route through a shared module or a defined contract.

## Abstraction quality

### Don't abstract until you have three users
The "rule of three": one occurrence is unique, two is coincidence, three is a pattern. Premature abstraction adds indirection without clarity. Flag a base class / generic helper with one subclass / one caller.

### Wrappers that pass through are noise
```kotlin
// BAD: forwards every call unchanged
class UserRepoWrapper(private val inner: UserRepo) : UserRepo {
    override fun find(id: UserId) = inner.find(id)
    override fun save(u: User) = inner.save(u)
}
```
Either add value (caching, logging, metrics, retry) or delete it.

### Leaky abstractions
An interface called `Repository` whose method names are SQL keywords (`select`, `join`) leaks the implementation. Name in the language of the domain.

## State and singletons

- Mutable global state is Major. Replace with constructor-injected collaborators or scoped lifecycles.
- A singleton holding a long-lived reference to a short-lived context (Activity, request, scope) leaks memory — Major.
- `object` with `var` properties shared across threads with no synchronisation is Critical.

## Visibility

- Default to `internal` for module-internal types and `private` for class-internal members.
- A `public` member that is only used inside the file is over-exposed — flag.
- A `protected` member on a `final` class has no purpose — flag.

## Boundaries and contracts

- Each public method should have one obvious failure mode and document it (KDoc `@throws`, `Result`, or sealed return).
- A method that throws three different exceptions for three different reasons is a candidate for a sealed `Outcome` return.
- "Returns null on any error" is a weak contract — callers can't distinguish "not found" from "transient failure".

## Anti-patterns

| Pattern | Why it's bad | Replacement |
|---------|-------------|-------------|
| Service locator (`ServiceLocator.get<T>()`) | hides dependencies, untestable | constructor injection |
| Static factory holding state | shared mutable global | non-static factory or DI |
| God class (Manager / Helper with 20 methods) | low cohesion | split by responsibility |
| Inheritance to share code | tight coupling, fragile | composition + delegation |
| Anemic domain model (data classes + service that does everything) | logic far from data | move behaviour onto the type that owns the invariant |
| `Pair`/`Triple` in public API | unnamed positional fields | named data class |
| Boolean parameters that change behaviour | unreadable call sites | enum, sealed type, or two methods |

## Layering smells specific to Kotlin / mobile / server

- A "ViewModel"/"Controller" reaching directly to a database / HTTP client without a repository or use case in between → Major.
- A repository returning UI types (e.g. resource IDs, formatted strings) → bleeding presentation into data.
- A domain type implementing a framework interface (annotation, parcelable, serialisation) → couples domain to a delivery mechanism. Push the annotation onto a DTO.
- A `data class` whose constructor calls a real I/O operation → constructors must not perform I/O.

## Configuration

- Configuration values resolved at runtime (env vars, build config) belong behind a typed interface, not scattered as raw strings.
- Magic numbers / strings used in more than one place → extract to a `const val` or sealed enum.
