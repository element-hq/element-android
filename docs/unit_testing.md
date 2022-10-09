# Table of Contents

<!--- TOC -->  

* [Overview](#overview)
  * [Best Practices](#best-practices)
* [Project Conventions](#project-conventions)
    * [Setup](#setup)
    * [Naming](#naming)
    * [Format](#format)
    * [Assertions](#assertions)
    * [Constants](#constants)
    * [Mocking](#mocking)
    * [Fakes](#fakes)
    * [Fixtures](#fixtures)
  * [Examples](#examples)
      * [Extensions used to streamline the test setup](#extensions-used-to-streamline-the-test-setup)
      * [Fakes and Fixtures](#fakes-and-fixtures)

<!--- END -->  

## Overview

Unit tests are a mechanism to validate our code executes the way we expect. They help to inform the design of our systems by requiring testability and
understanding, they describe the inner workings without relying on inline comments and protect from unexpected regressions.

However, unit tests are not a magical solution to solve all our problems and come at a cost. Unreliable and hard to maintain tests often end up ignored, deleted
or worse, provide a false sense of security.

### Best Practices

Tests can be written in many ways, the main rule is to keep them simple and maintainable. Some ways to help achieve this are...

- Break out logic into single units (following the Single Responsibility Principle) to reduce test complexity.
- Favour pure functions, avoiding mutable state.
- Prefer dependency injection to static calls to allow for simpler test setup.
- Write concise tests with a single function under test, clearly showing the inputs and expected output.
- Create separate test cases instead of changing parameters and grouping multiple assertions within a single test to help trace back failure causes (with the
  exception of parameterised tests).
- Assert against entire models instead of subsets of properties to capture any possible changes within the test scope.
- Avoid invoking logic from production instances other than the class under test to guard from unrelated changes.
- Always inject `Dispatchers` and `Clock` instances and provide fake implementations for tests to avoid non deterministic results.

## Project Conventions

#### Setup

- Test file and class name should be the class under test with the Test suffix, created in a `test` sourceset, with the same package name as the class under
  test.
- Dependencies of the class are instantiated inline, junit will recreate the test class for each test run.
- A line break between the dependencies and class under test helps clarify the instance being tested.

```kotlin

class MyClassTest {

    private val fakeUppercaser = FakeUppercaser()

    // line break between the class under test and its dependencies
    private val myClass = MyClass(fakeUppercaser.instance)
}

```  

#### Naming

- Test names use the `Gherkin` format, `given, when, then` mapping to the input, logic under test and expected result.
    - `given` - Uniqueness about the environment or dependencies in which the test case is running. _"given device is android 12 and supports dark mode"_
    - `when` - The action/function under test. _"when reading dark mode status"_
    - `then` - The expected result from the combination of _given_ and _when_. _"then returns dark mode enabled"_
- Test names are written using kotlin back ticks to enable sentences _ish_.

```kotlin
@Test
fun `given a lowercase label, when uppercasing, then returns label uppercased`
```

When the input is given directly to the _when_, this can also be represented as...

```kotlin
@Test
fun `when uppercasing a lowercase label, then returns label uppercased`
```

Multiple given or returns statements can be used in the name although it could be a sign that the logic being tested does too much.

---

#### Format

- Test bodies are broken into sections through the use of blank lines where the sections correspond to the test name.
- Sections can span multiple lines.

```kotlin 
// comments are for illustrative purposes
/* given */ val lowercaseLabel = "hello world" 

/* when */ val result = textUppercaser.uppercase(lowercaseLabel)

/* then */ result shouldBeEqualTo "HELLO WORLD"
```

- Functions extracted from test bodies are placed beneath all the unit tests.

---

#### Assertions

- Assertions against test results are made using [Kluent's](https://github.com/MarkusAmshove/Kluent) _fluent_ api.
- Typically `shouldBeEqualTo`is the main assertion to use for asserting function return values as by project convention we assert against entire objects or
  lists.

```kotlin
val result = listOf("hello", "world")

// Fail
result shouldBeEqualTo listOf("hello")
```

```kotlin
data class Person(val age: Int, val name: String)

val result = Person(age = 100, name = "Gandalf")

// Avoid
result.age shouldBeEqualTo 100

// Prefer
result shouldBeEqualTo Person(age = 100, "Gandalf")
```

- Exception throwing can be asserted against using `assertFailsWith<T : Throwable>`.
- When asserting reusable exceptions, include the message to distinguish between them.

```kotlin
assertFailsWith<ConcreteException>(message = "Details about error") {
    // when section of the test
    codeUnderTest()
}
```

---

#### Constants

- Reusable values are extracted to file level immutable properties or constants.
- These can be parameters or expected results.
- The naming convention is to prefix with `A` or `AN` for better matching with the test name.

```kotlin
private const val A_LOWERCASE_LABEL = "hello"

class MyTest {
    @Test
    fun `when uppercasing a lowercase label, then returns label uppercased`() {
        val result = TextUppercaser().uppercase(A_LOWERCASE_LABEL)
        ...
    }
}
```

---

#### Mocking

- In order to provide different behaviour for dependencies within tests our main method is through mocking, using [Mockk](https://mockk.io/).
- We avoid using relaxed mocks in favour of explicitly declaring mock behaviour through the _Fake_ convention. There are exceptions when mocking framework
  classes which would require a lot of boilerplate.
- Using `Spy` is discouraged as it inherently requires real instances, which we are avoiding in our tests. There are exceptions such as `VectorFeatures` which
  acts like a `Fixture` in release builds.

---

#### Fakes

- Fakes are reusable instances of classes purely for testing purposes. They provide functions to replace the functions of the interface/class they're faking
  with test specific values.
- When faking an interface, the _Fake_ can be written using delegation or by stubbing
- All Fakes currently reside in the same package `${package}.test.fakes`

```kotlin
// Delegating to a mock
class FakeClock : Clock by mockk() {
    fun givenEpoch(epoch: Long) {
        every { epochMillis() } returns epoch
    }
}

// Stubbing the interface
class FakeClock(private val epoch: Long) : Clock {
    override fun epochMillis() = epoch
}
```  

It's currently more common for fakes to fake class behaviour, we achieve this by wrapping and exposing a mock instance.

```kotlin
class FakeCursor {
    val instance = mockk<Cursor>()
    fun givenEmpty() {
        every { instance.count } returns 0
        every { instance.moveToFirst() } returns false
    }
}

val fakeCursor = FakeCursor().apply { givenEmpty() }
```

#### Fixtures

- Fixtures are a reusable wrappers around data models. They provide default values to make creating instances as easy as possible, with the option to override
  specific parameters when needed.
- Are namespaced within an `object`.
- Reduces the _find usages_ noise when searching for usages of the origin class construction.
- All Fixtures currently reside in the same package `${package}.test.fixtures`.

```kotlin
object ContentAttachmentDataFixture {
    fun aContentAttachmentData(
            type: ContentAttachmentData.Type.TEXT,
            mimeType: String? = null
    ) = ContentAttachmentData(type, mimeType)
}
```

- Fixtures can also be used to manage specific combinations of parameters

```kotlin
fun aContentAttachmentAudioData() = aContentAttachmentData(
        type = ContentAttachmentData.Type.AUDIO,
        mimeType = "audio/mp3",
)
```

--- 

### Examples

##### Extensions used to streamline the test setup

```kotlin
class CircularCacheTest {

    @Test
    fun `when putting more than cache size then cache is limited to cache size`() {
        val (cache, internalData) = createIntCache(cacheSize = 3)

        cache.putInOrder(1, 1, 1, 1, 1, 1)

        internalData shouldBeEqualTo arrayOf(1, 1, 1)
    }
}

private fun createIntCache(cacheSize: Int): Pair<CircularCache<Int>, Array<Int?>> {
    var internalData: Array<Int?>? = null
    val factory: (Int) -> Array<Int?> = {
        Array<Int?>(it) { null }.also { array -> internalData = array }
    }
    return CircularCache(cacheSize, factory) to internalData!!
}

private fun CircularCache<Int>.putInOrder(vararg values: Int) {
    values.forEach { put(it) }
}
```

##### Fakes and Fixtures

```kotlin
class LateInitUserPropertiesFactoryTest {

    private val fakeActiveSessionDataSource = FakeActiveSessionDataSource()
    private val fakeVectorStore = FakeVectorStore()
    private val fakeContext = FakeContext()
    private val fakeSession = FakeSession().also {
        it.givenVectorStore(fakeVectorStore.instance)
    }

    private val lateInitUserProperties = LateInitUserPropertiesFactory(
            fakeActiveSessionDataSource.instance,
            fakeContext.instance
    )

    @Test
    fun `given no active session, when creating properties, then returns null`() {
        val result = lateInitUserProperties.createUserProperties()

        result shouldBeEqualTo null
    }

    @Test
    fun `given a teams use case set on an active session, when creating properties, then includes the remapped WorkMessaging selection`() {
        fakeVectorStore.givenUseCase(FtueUseCase.TEAMS)
        fakeActiveSessionDataSource.setActiveSession(fakeSession)

        val result = lateInitUserProperties.createUserProperties()

        result shouldBeEqualTo UserProperties(
                ftueUseCaseSelection = UserProperties.FtueUseCaseSelection.WorkMessaging
        )
    }
}
  ```

##### ViewModel

- `ViewModels` tend to be one of the most complex areas to unit test due to their position as a coordinator of data flows and bridge between domains.
- As the project uses a slightly tweaked`MvRx`, our API for the `ViewModel` is simplified down to `input - ViewModel.handle(Action)`
  and `output Flows - ViewModel.viewEvents & ViewModel.stateFlow`. A `ViewModel` test asserter has been created to further simplify the process.

```kotlin
class ViewModelTest {

    private var initialState = ViewState.Empty

    @get:Rule
    val mavericksTestRule = MavericksTestRule(testDispatcher = UnconfinedTestDispatcher())

    @Test
    fun `when handling MyAction, then emits Loading and Content states`() {
        val viewModel = ViewModel<State>(initialState)
        val test = viewModel.test() // must be invoked before interacting with the VM 

        viewModel.handle(MyAction)

        test
                .assertViewStates(initialState, State.Loading, State.Content())
                .assertNoEvents()
                .finish()
    }
}
```

- `ViewModels` often emit multiple states which are copies of the previous state, the `test` extension `assertStatesChanges` allows only the difference to be
  supplied.

```kotlin
data class ViewState(val name: String? = null, val age: Int? = null)
val initialState = ViewState()
val viewModel = ViewModel<State>(initialState)
val test = viewModel.test()

viewModel.handle(ChangeNameAction("Gandalf"))

test
        .assertStatesChanges(
                initialState,
                { copy(name = "Gandalf") },
        )
        .finish()
```
