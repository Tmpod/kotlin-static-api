# Static API generator for Kotlin

A simple KSP annotation processor that generates static objects implementing a given API interface, with swappable implementation delegates.

## Usage

Add the KSP plugin to your Gradle script:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.3.4"
}
```

And add the annotations and the processor to your dependencies:

```kotlin
dependencies {
    implementation("dev.tmpod:staticapi-annotations:0.1")
    ksp("dev.tmpod:staticapi-processor:0.1")
}
```

Alternatively, you can add these lines to your version catalog (`libs.versions.toml`)

```yaml
[versions]
# ...
ksp = "2.3.4"
static-api = "0.1"

[libraries]
# ...
staticApi-annotations = { module = "dev.tmpod:static-api-annotations", version.ref = "static-api" }
staticApi-processor = { module = "dev.tmpod:static-api-processor", version.ref = "static-api" }

[plugins]
# ...
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

Now, declare an interface for your API (and document it), then annotate it with [`@StaticApi`](./annotations/src/main/kotlin/StaticApi.kt).

```kotlin
/**
 * API to work with foo and bar.
 */
interface IFooBar {
    /** Makes a new foo. */
    fun makeFoo(): Foo

    /** Returns true if the given foo is also a bar. */
    fun isBar(foo: Foo): Boolean
}
```

The KSP processor will generate an object roughly equivalent to the following:

```kotlin
/**
 * API to work with foo and bar.
 */
object FooBar {
    @ApiStatus.Internal lateinit var delegate: IFooBar

    /** Makes a new foo. */
    @JvmStatic fun makeFoo() = delegate.makeFoo()

    /** Returns true if the given foo is also a bar. */
    @JvmStatic fun isBar(foo: Foo) = delegate.isBar(foo)
}
```

You can then use it like `FooBar.makeFoo()` in both Java and Kotlin.
